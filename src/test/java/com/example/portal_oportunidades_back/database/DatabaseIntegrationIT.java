package com.example.portal_oportunidades_back.database;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@Transactional
class DatabaseIntegrationIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired JdbcClient jdbcClient;
    @Autowired OpportunityService opportunityService;
    @Autowired OpportunityRepository opportunityRepository;
    @Autowired EntityManagerFactory entityManagerFactory;
    private Long recruiterId;

    @BeforeEach
    void createAuthorizedRecruiter() {
        String suffix = UUID.randomUUID().toString();
        recruiterId = jdbcClient.sql("""
                        INSERT INTO app_auth.usuario
                            (name, email, senha_hash, active, criado_em, atualizado_em)
                        VALUES (:name, :email, 'not-a-real-password', true, now(), now())
                        RETURNING id
                        """)
                .param("name", "Integration Recruiter")
                .param("email", "integration-" + suffix + "@example.test")
                .query(Long.class).single();
        jdbcClient.sql("""
                        INSERT INTO perfil.recrutador
                            (usuario_id, tipo, nome_organizacao, autorizado, criado_em, atualizado_em)
                        VALUES (:id, CAST('COMPANY' AS perfil.tipo_recrutador), 'Integration Organization', true, now(), now())
                        """)
                .param("id", recruiterId).update();
    }

    @Test
    void migrationsCreateVersionColumnAndHibernateValidatesTheMappedSchema() {
        assertThat(entityManagerFactory).isNotNull();
        assertThat(jdbcClient.sql("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'oportunidades' AND table_name = 'oportunidade'
                  AND column_name = 'version' AND data_type = 'bigint'
                """).query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbcClient.sql("SELECT count(*) FROM databasechangelog WHERE id = '20261002-opportunity-version'")
                .query(Long.class).single()).isEqualTo(1L);

        OpportunityResponse created = create("Versioned schema opportunity");
        Long initialVersion = jdbcClient.sql("SELECT version FROM oportunidades.oportunidade WHERE id = :id")
                .param("id", created.id()).query(Long.class).single();
        opportunityService.update(recruiterId, created.id(), new com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest(
                "Version updated", created.description(), created.requirements(), created.modality(),
                created.location(), created.vacancyCount(), created.registrationStartsAt(), created.registrationEndsAt()));
        opportunityRepository.flush();
        Long updatedVersion = jdbcClient.sql("SELECT version FROM oportunidades.oportunidade WHERE id = :id")
                .param("id", created.id()).query(Long.class).single();
        assertThat(updatedVersion).isGreaterThan(initialVersion);
    }

    @Test
    void publicFiltersAndCursorPaginationWorkAgainstPostgresql() {
        OpportunityResponse first = create("Research Java 100% assistant");
        OpportunityResponse second = create("Research Java 100% intern");
        create("Research Java 100X assistant");
        opportunityService.publish(recruiterId, first.id());
        opportunityService.publish(recruiterId, second.id());

        OpportunityFilter filter = new OpportunityFilter("java 100%", OpportunityModality.INTERNSHIP,
                OpportunityStatus.PUBLISHED, recruiterId);
        var pageOne = opportunityService.listPublic(filter, null, 1);
        assertThat(pageOne.items()).hasSize(1);
        assertThat(pageOne.hasNext()).isTrue();
        var pageTwo = opportunityService.listPublic(filter, pageOne.nextCursor(), 1);
        assertThat(pageTwo.items()).hasSize(1);
        assertThat(pageTwo.items().getFirst().id()).isNotEqualTo(pageOne.items().getFirst().id());
        assertThat(pageTwo.hasNext()).isFalse();
    }

    @Test
    void publicDetailsReturnNotFoundForMissingAndSoftDeletedRecords() {
        assertThatThrownBy(() -> opportunityService.findPublicById(-1L))
                .isInstanceOf(ResourceNotFoundException.class);
        OpportunityResponse published = create("Public opportunity");
        opportunityService.publish(recruiterId, published.id());
        assertThat(opportunityService.findPublicById(published.id()).id()).isEqualTo(published.id());
        opportunityService.delete(recruiterId, published.id());
        assertThatThrownBy(() -> opportunityService.findPublicById(published.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private OpportunityResponse create(String title) {
        Instant startsAt = Instant.now().plusSeconds(3600);
        return opportunityService.create(recruiterId, new OpportunityCreateRequest(title,
                "Integration test opportunity", "Java and Spring", OpportunityModality.INTERNSHIP,
                "Remote", 2, startsAt, startsAt.plusSeconds(86_400)));
    }
}
