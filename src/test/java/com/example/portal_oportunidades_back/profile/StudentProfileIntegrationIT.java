package com.example.portal_oportunidades_back.profile;

import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.entity.Student;
import com.example.portal_oportunidades_back.profile.repository.StudentRepository;
import com.example.portal_oportunidades_back.profile.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
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
class StudentProfileIntegrationIT {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired StudentProfileService profiles;
    @Autowired StudentResumeService resumes;
    @Autowired StudentRepository students;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactions;
    private Long userId;

    @BeforeEach
    void createUser() { userId = user(); }

    @Test
    void createsReadsUpdatesAndReplacesExperiencesWithoutChangingIdentity() {
        var created = profiles.updateProfile(userId, request("Computação", List.of(item(null, "Estagiário"))));
        assertThat(created.id()).isEqualTo(userId);
        assertThat(created.name()).isEqualTo("José");
        assertThat(created.summary()).hasSize(500);
        Long experienceId = created.experiences().getFirst().id();
        var updated = profiles.updateProfile(userId, request("Engenharia", List.of(
                item(experienceId, "Pesquisador"), item(null, "Assistente"))));
        assertThat(updated.experiences()).hasSize(2);
        assertThat(updated.experiences()).anySatisfy(experience -> {
            assertThat(experience.id()).isEqualTo(experienceId);
            assertThat(experience.position()).isEqualTo("Pesquisador");
        });
        profiles.updateProfile(userId, request("Engenharia", List.of()));
        assertThat(profiles.getProfile(userId).experiences()).isEmpty();
        assertThat(jdbc.sql("SELECT senha_hash FROM app_auth.usuario WHERE id = :id")
                .param("id", userId).query(String.class).single()).isEqualTo("not-a-password");
    }

    @Test
    void rejectsForeignExperienceAndPreservesBothProfiles() {
        Long other = user();
        var ownedByOther = profiles.updateProfile(other, requestFor(other, "Other", List.of(item(null, "Other job"))));
        profiles.updateProfile(userId, request("Original", List.of(item(null, "My job"))));
        Long foreignId = ownedByOther.experiences().getFirst().id();
        assertThatThrownBy(() -> profiles.updateProfile(userId, request("Changed", List.of(item(foreignId, "Stolen")))))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(profiles.getProfile(userId).course()).isEqualTo("Original");
        assertThat(profiles.getProfile(userId).experiences().getFirst().position()).isEqualTo("My job");
        assertThat(profiles.getProfile(other).experiences().getFirst().position()).isEqualTo("Other job");
    }

    @Test
    void rollsBackProfileAndEarlierExperienceInsertWhenLaterExperienceIsInvalid() {
        profiles.updateProfile(userId, request("Original", List.of()));
        var invalid = new ExperienceRequest(null, "Invalid", "Company", LocalDate.now().plusDays(10),
                null, true, null);
        assertThatThrownBy(() -> profiles.updateProfile(userId,
                request("Changed", List.of(item(null, "First valid"), invalid))))
                .isInstanceOf(BusinessException.class);
        assertThat(profiles.getProfile(userId).course()).isEqualTo("Original");
        assertThat(profiles.getProfile(userId).experiences()).isEmpty();
    }

    @Test
    void rejectsDuplicateRegistrationAndUnknownUser() {
        profiles.updateProfile(userId, request("Course", List.of()));
        Long other = user();
        assertThatThrownBy(() -> profiles.updateProfile(other, request("Duplicate", List.of())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> profiles.updateProfile(-1L, requestFor(-1L, "Course", List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> profiles.getProfile(other)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resumeContainsPersistedLatestDataWithoutExposingPasswordOrRegistration() throws Exception {
        profiles.updateProfile(userId, request("Computação", List.of(item(null, "Pesquisador"))));
        profiles.updateProfile(userId, request("Engenharia", List.of()));
        try (var pdf = Loader.loadPDF(resumes.generate(userId))) {
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("José", "Engenharia").doesNotContain("Pesquisador", "not-a-password", "reg-" + userId);
        }
    }

    @Test
    void migrationAddsTextColumnsVersionAndDatabaseConstraints() {
        assertThat(jdbc.sql("SELECT count(*) FROM databasechangelog WHERE id = '20261009-student-profile'")
                .query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("""
                SELECT count(*) FROM information_schema.columns WHERE table_schema = 'perfil'
                AND table_name = 'aluno' AND column_name IN ('resumo','habilidades','interesses') AND data_type = 'text'
                """).query(Long.class).single()).isEqualTo(3);
        profiles.updateProfile(userId, request("Course", List.of()));
        assertThatThrownBy(() -> jdbc.sql("UPDATE perfil.aluno SET periodo = 0 WHERE usuario_id = :id")
                .param("id", userId).update()).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void staleProfileUpdateCannotOverwriteCommittedUpdate() throws Exception {
        profiles.updateProfile(userId, request("Original", List.of()));
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> stale = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                Student student = students.findById(userId).orElseThrow();
                loaded.countDown();
                await(committed);
                student.updateProfile("reg-" + userId, "Stale", null, null, null, null, null);
            }));
            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
                profiles.updateProfile(userId, request("Committed", List.of()));
            } finally { committed.countDown(); }
            assertThatThrownBy(() -> stale.get(15, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        }
        assertThat(profiles.getProfile(userId).course()).isEqualTo("Committed");
    }

    @Test
    void concurrentExperienceOnlyUpdatesCannotLoseCommittedExperiences() throws Exception {
        profiles.updateProfile(userId, request("Same course", List.of()));
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> stale = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                students.findById(userId).orElseThrow();
                loaded.countDown();
                await(committed);
                profiles.updateProfile(userId, request("Same course", List.of(item(null, "Stale job"))));
            }));
            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
                profiles.updateProfile(userId, request("Same course", List.of(item(null, "Committed job"))));
            } finally { committed.countDown(); }
            assertThatThrownBy(() -> stale.get(15, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        }
        assertThat(profiles.getProfile(userId).experiences()).singleElement()
                .extracting(ExperienceResponse::position).isEqualTo("Committed job");
    }

    @Test
    void initialCreationRollsBackWhenExperienceIsInvalid() {
        var invalid = new ExperienceRequest(null, "Invalid", "Company", LocalDate.now().plusDays(10),
                null, true, null);
        assertThatThrownBy(() -> profiles.updateProfile(userId, request("Course", List.of(invalid))))
                .isInstanceOf(BusinessException.class);
        assertThat(students.existsById(userId)).isFalse();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private StudentProfileRequest request(String course, List<ExperienceRequest> experiences) {
        return requestFor(userId, course, experiences);
    }

    private StudentProfileRequest requestFor(Long id, String course, List<ExperienceRequest> experiences) {
        return new StudentProfileRequest("reg-" + id, course, (short) 3, "9999",
                "R".repeat(500), "Java", "Pesquisa", experiences);
    }

    private ExperienceRequest item(Long id, String position) {
        return new ExperienceRequest(id, position, "Universidade", LocalDate.of(2025, 1, 1),
                null, true, "D".repeat(500));
    }

    private Long user() {
        return jdbc.sql("""
                INSERT INTO app_auth.usuario (name, email, senha_hash, active, criado_em, atualizado_em)
                VALUES ('José', :email, 'not-a-password', true, now(), now()) RETURNING id
                """).param("email", UUID.randomUUID() + "@example.test").query(Long.class).single();
    }
}
