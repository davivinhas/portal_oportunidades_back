package com.example.portal_oportunidades_back.database;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class DatabaseIntegrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private OpportunityRepository opportunityRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void appliesMigrationsAndPersistsOpportunityWithNativeEnums() {
        Long recruiterId = createAuthorizedRecruiter();

        Instant startsAt = Instant.now().plusSeconds(3_600);
        OpportunityResponse created = opportunityService.create(recruiterId, new OpportunityCreateRequest(
                "Backend Internship", "Integration test opportunity", "Java and Spring",
                OpportunityModality.INTERNSHIP, "Remote", 2, startsAt, startsAt.plusSeconds(86_400)
        ));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(OpportunityStatus.DRAFT);
        assertThat(opportunityRepository.findById(created.id())).isPresent();

        OpportunityResponse published = opportunityService.publish(recruiterId, created.id());

        assertThat(published.status()).isEqualTo(OpportunityStatus.PUBLISHED);
        assertThat(opportunityService.listOwned(recruiterId))
                .extracting(OpportunityResponse::id)
                .containsExactly(created.id());
    }

    @Test
    void rejectsStaleEditWhenOpportunityIsPublishedConcurrently() throws Exception {
        Long recruiterId = createAuthorizedRecruiter();
        Instant startsAt = Instant.now().plusSeconds(3_600);
        OpportunityResponse created = opportunityService.create(recruiterId, new OpportunityCreateRequest(
                "Original title", "Original description", "Java",
                OpportunityModality.INTERNSHIP, "Remote", 1, startsAt, startsAt.plusSeconds(86_400)
        ));

        CountDownLatch editLoaded = new CountDownLatch(1);
        CountDownLatch publicationCommitted = new CountDownLatch(1);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<?> staleEdit = executor.submit(() -> transaction.executeWithoutResult(status -> {
                Opportunity opportunity = opportunityRepository.findById(created.id()).orElseThrow();
                editLoaded.countDown();
                await(publicationCommitted);
                opportunity.updateDetails("Stale title", "Stale description", "Java",
                        OpportunityModality.INTERNSHIP, "Remote", 1,
                        startsAt, startsAt.plusSeconds(86_400));
            }));

            assertThat(editLoaded.await(5, TimeUnit.SECONDS)).isTrue();
            opportunityService.publish(recruiterId, created.id());
            publicationCommitted.countDown();

            assertThatThrownBy(() -> staleEdit.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ObjectOptimisticLockingFailureException.class);

            Opportunity persisted = opportunityRepository.findById(created.id()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(OpportunityStatus.PUBLISHED);
            assertThat(persisted.getTitle()).isEqualTo("Original title");
        } finally {
            publicationCommitted.countDown();
            executor.shutdownNow();
        }
    }

    private Long createAuthorizedRecruiter() {
        Long recruiterId = jdbcClient.sql("""
                        INSERT INTO app_auth.usuario
                            (name, email, senha_hash, active, criado_em, atualizado_em)
                        VALUES
                            ('Integration Recruiter', :email, 'not-a-real-password', true, now(), now())
                        RETURNING id
                        """)
                .param("email", "integration-" + UUID.randomUUID() + "@example.test")
                .query(Long.class)
                .single();

        jdbcClient.sql("""
                        INSERT INTO perfil.recrutador
                            (usuario_id, tipo, nome_organizacao, autorizado, criado_em, atualizado_em)
                        VALUES
                            (:id, CAST('COMPANY' AS perfil.tipo_recrutador), 'Integration Organization', true, now(), now())
                        """)
                .param("id", recruiterId)
                .update();
        return recruiterId;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent operation");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent operation", exception);
        }
    }
}
