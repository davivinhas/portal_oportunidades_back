package com.example.portal_oportunidades_back.application;

import com.example.portal_oportunidades_back.application.repository.ApplicationRepository;
import com.example.portal_oportunidades_back.application.service.ApplicationService;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class ApplicationIntegrationIT {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcClient jdbc;
    @Autowired ApplicationService applications;
    @Autowired ApplicationRepository repository;
    @Autowired OpportunityRepository opportunities;
    @Autowired OpportunityService opportunityService;
    @Autowired PlatformTransactionManager transactions;
    private Long studentId;
    private Long otherStudentId;
    private Long recruiterId;
    private Long opportunityId;

    @BeforeEach
    void createProfilesAndOpportunity() {
        studentId = student();
        otherStudentId = student();
        recruiterId = user();
        jdbc.sql("""
                INSERT INTO perfil.recrutador (usuario_id, tipo, nome_organizacao, autorizado, criado_em, atualizado_em)
                VALUES (:id, 'COMPANY', 'Test company', true, now(), now())
                """).param("id", recruiterId).update();
        opportunityId = opportunity();
    }

    @Test
    void createsWithAuditDataAndPreservesCancellationAndUniqueness() {
        var created = applications.create(opportunityId, studentId);
        assertThat(created.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();
        assertThat(created.appliedAt()).isNotNull();
        assertThat(created.opportunity().organizationName()).isEqualTo("Test company");
        applications.cancel(studentId, created.id());
        applications.cancel(studentId, created.id());
        assertThat(applications.getOwned(studentId, created.id()).status()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThatThrownBy(() -> applications.create(opportunityId, studentId)).isInstanceOf(ConflictException.class);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void paginatesFiltersAndIsolatesStudents() {
        var first = applications.create(opportunityId, studentId);
        var second = applications.create(opportunity(), studentId);
        applications.create(opportunityId, otherStudentId);
        applications.cancel(studentId, first.id());
        var page = applications.listOwned(studentId, null, 0, 1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
        assertThat(applications.listOwned(studentId, null, 1, 1).items().getFirst().id())
                .isNotEqualTo(page.items().getFirst().id());
        assertThat(applications.listOwned(studentId, ApplicationStatus.CANCELLED, 0, 20).items())
                .singleElement().extracting(item -> item.id()).isEqualTo(first.id());
        assertThat(applications.listOwned(studentId, ApplicationStatus.SUBMITTED, 0, 20).items())
                .singleElement().extracting(item -> item.id()).isEqualTo(second.id());
        assertThatThrownBy(() -> applications.getOwned(otherStudentId, first.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> applications.cancel(otherStudentId, first.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsUnavailableOpportunityAndMissingProfile() {
        assertThatThrownBy(() -> applications.create(opportunityId, recruiterId)).isInstanceOf(ResourceNotFoundException.class);
        jdbc.sql("UPDATE oportunidades.oportunidade SET fim_inscricoes = now() - interval '1 second' WHERE id = :id")
                .param("id", opportunityId).update();
        assertThatThrownBy(() -> applications.create(opportunityId, studentId)).isInstanceOf(BusinessException.class);
        opportunityService.delete(recruiterId, opportunityId);
        assertThatThrownBy(() -> applications.create(opportunityId, studentId)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(count()).isZero();
    }

    @Test
    void preservesApplicationsAfterOpportunityClosesOrIsSoftDeleted() {
        var created = applications.create(opportunityId, studentId);
        opportunityService.close(recruiterId, opportunityId);
        assertThat(applications.getOwned(studentId, created.id()).opportunity().status()).isEqualTo(OpportunityStatus.CLOSED);
        opportunityService.delete(recruiterId, opportunityId);
        assertThat(applications.getOwned(studentId, created.id()).opportunity().deletedAt()).isNotNull();
        assertThat(applications.listOwned(studentId, null, 0, 20).items()).hasSize(1);
    }

    @Test
    void duplicateRequestsCommitExactlyOneApplication() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<String> create = () -> {
                ready.countDown();
                await(start);
                try { applications.create(opportunityId, studentId); return "CREATED"; }
                catch (ConflictException exception) { return "CONFLICT"; }
            };
            Future<String> first = executor.submit(create);
            Future<String> second = executor.submit(create);
            try { assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); }
            finally { start.countDown(); }
            assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "CONFLICT");
        }
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void closingFirstBlocksApplicationUntilCommittedThenRejectsIt() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> closing = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                opportunityService.close(recruiterId, opportunityId);
                opportunities.flush();
                closed.countDown();
                await(release);
            }));
            Future<?> creating = null;
            try {
                assertThat(closed.await(10, TimeUnit.SECONDS)).isTrue();
                creating = executor.submit(() -> applications.create(opportunityId, studentId));
                awaitDatabaseLock();
                assertThat(creating.isDone()).isFalse();
            } finally { release.countDown(); }
            closing.get(15, TimeUnit.SECONDS);
            Future<?> attempt = creating;
            assertThatThrownBy(() -> attempt.get(15, TimeUnit.SECONDS)).hasCauseInstanceOf(BusinessException.class);
        }
        assertThat(count()).isZero();
    }

    @Test
    void applicationFirstCommitsBeforeConcurrentClosure() throws Exception {
        CountDownLatch created = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> creating = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                applications.create(opportunityId, studentId);
                created.countDown();
                await(release);
            }));
            Future<?> closing = null;
            try {
                assertThat(created.await(10, TimeUnit.SECONDS)).isTrue();
                closing = executor.submit(() -> opportunityService.close(recruiterId, opportunityId));
                awaitDatabaseLock();
                assertThat(closing.isDone()).isFalse();
            } finally { release.countDown(); }
            creating.get(15, TimeUnit.SECONDS);
            closing.get(15, TimeUnit.SECONDS);
        }
        assertThat(count()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT status::text FROM oportunidades.oportunidade WHERE id = :id")
                .param("id", opportunityId).query(String.class).single()).isEqualTo("CLOSED");
    }

    @Test
    void cancellationCannotOverwriteConcurrentApproval() throws Exception {
        var created = applications.create(opportunityId, studentId);
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch approved = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> cancelling = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                var application = repository.findByIdAndStudentId(created.id(), studentId).orElseThrow();
                loaded.countDown();
                await(approved);
                application.cancel();
            }));
            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
                new TransactionTemplate(transactions).executeWithoutResult(status ->
                        repository.findById(created.id()).orElseThrow().updateStatus(ApplicationStatus.APPROVED));
            } finally { approved.countDown(); }
            assertThatThrownBy(() -> cancelling.get(15, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        }
        assertThat(applications.getOwned(studentId, created.id()).status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThatThrownBy(() -> applications.cancel(studentId, created.id())).isInstanceOf(BusinessException.class);
    }

    @Test
    void migrationEnforcesUniquePairEvenWhenServiceIsBypassed() {
        var created = applications.create(opportunityId, studentId);
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO oportunidades.candidatura (aluno_id, oportunidade_id, status, data_candidatura, criado_em, atualizado_em)
                VALUES (:student, :opportunity, 'SUBMITTED', now(), now(), now())
                """).param("student", studentId).param("opportunity", opportunityId).update())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.sql("SELECT version FROM oportunidades.candidatura WHERE id = :id")
                .param("id", created.id()).query(Long.class).single()).isNotNull();
    }

    private void awaitDatabaseLock() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            long waiting = jdbc.sql("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                    AND query LIKE '%oportunidade%'
                    """).query(Long.class).single();
            if (waiting > 0) return;
            Thread.sleep(20);
        }
        throw new AssertionError("Expected the concurrent transaction to wait for the opportunity lock");
    }

    private void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private Long opportunity() {
        return jdbc.sql("""
                INSERT INTO oportunidades.oportunidade (titulo, descricao, modalidade, quantidade_vagas,
                    inicio_inscricoes, fim_inscricoes, status, recrutador_id, criado_em, atualizado_em)
                VALUES ('Test opportunity', 'Description', 'INTERNSHIP', 2,
                    now() - interval '1 hour', now() + interval '1 day', 'PUBLISHED', :recruiter, now(), now())
                RETURNING id
                """).param("recruiter", recruiterId).query(Long.class).single();
    }

    private Long student() {
        Long id = user();
        jdbc.sql("""
                INSERT INTO perfil.aluno (usuario_id, matricula, curso, criado_em, atualizado_em)
                VALUES (:id, :registration, 'Computer Science', now(), now())
                """).param("id", id).param("registration", "test-" + id).update();
        return id;
    }

    private Long user() {
        return jdbc.sql("""
                INSERT INTO app_auth.usuario (name, email, senha_hash, active, criado_em, atualizado_em)
                VALUES ('Application test', :email, 'not-a-password', true, now(), now()) RETURNING id
                """).param("email", UUID.randomUUID() + "@example.test").query(Long.class).single();
    }

    private Long count() {
        return jdbc.sql("SELECT count(*) FROM oportunidades.candidatura WHERE aluno_id = :id AND oportunidade_id = :opportunity")
                .param("id", studentId).param("opportunity", opportunityId).query(Long.class).single();
    }
}
