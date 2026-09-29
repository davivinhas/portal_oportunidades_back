package com.example.portal_oportunidades_back.database;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void appliesMigrationsAndPersistsOpportunityWithNativeEnums() {
        Long recruiterId = jdbcClient.sql("""
                        INSERT INTO app_auth.usuario
                            (name, email, senha_hash, active, criado_em, atualizado_em)
                        VALUES
                            ('Integration Recruiter', 'integration@example.test', 'not-a-real-password', true, now(), now())
                        RETURNING id
                        """)
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
}
