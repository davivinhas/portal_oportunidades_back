package com.example.portal_oportunidades_back.profile;

import com.example.portal_oportunidades_back.profile.service.RecruiterFollowService;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
class RecruiterFollowIntegrationIT {
    @Container @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Autowired JdbcClient jdbc;
    @Autowired RecruiterFollowService service;
    private Long studentId;
    private Long recruiterId;

    @BeforeEach
    void createProfiles() {
        studentId = createUser();
        recruiterId = createUser();
        jdbc.sql("""
                INSERT INTO perfil.aluno (usuario_id, matricula, curso, criado_em, atualizado_em)
                VALUES (:id, :registration, 'Computer Science', now(), now())
                """).param("id", studentId).param("registration", "test-" + studentId).update();
        jdbc.sql("""
                INSERT INTO perfil.recrutador (usuario_id, tipo, nome_organizacao, autorizado, criado_em, atualizado_em)
                VALUES (:id, 'COMPANY', 'Test company', true, now(), now())
                """).param("id", recruiterId).update();
    }

    @Test
    void repeatedOperationsPreserveOneLinkAndAllowFollowingAgain() {
        assertThat(service.getFollowing(studentId, recruiterId).following()).isFalse();
        service.follow(studentId, recruiterId);
        service.follow(studentId, recruiterId);
        assertThat(linkCount()).isEqualTo(1L);
        assertThat(service.getFollowing(studentId, recruiterId).following()).isTrue();
        service.unfollow(studentId, recruiterId);
        service.unfollow(studentId, recruiterId);
        assertThat(linkCount()).isZero();
        assertThat(service.getFollowing(studentId, recruiterId).following()).isFalse();
        service.follow(studentId, recruiterId);
        assertThat(linkCount()).isEqualTo(1L);
    }

    @Test
    void simultaneousFollowRequestsCommitWithoutDuplicates() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> follow = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                service.follow(studentId, recruiterId);
                return null;
            };
            Future<Void> first = executor.submit(follow);
            Future<Void> second = executor.submit(follow);
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        }
        assertThat(linkCount()).isEqualTo(1L);
    }

    @Test
    void rejectsUserWithoutStudentProfile() {
        assertThatThrownBy(() -> service.follow(recruiterId, recruiterId))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Student not found");
        assertThat(linkCount()).isZero();
    }

    private long linkCount() {
        return jdbc.sql("SELECT count(*) FROM perfil.recruiter_follow WHERE student_id = :student AND recruiter_id = :recruiter")
                .param("student", studentId).param("recruiter", recruiterId).query(Long.class).single();
    }

    private Long createUser() {
        return jdbc.sql("""
                INSERT INTO app_auth.usuario (name, email, senha_hash, active, criado_em, atualizado_em)
                VALUES ('Follow test', :email, 'not-a-password', true, now(), now()) RETURNING id
                """).param("email", UUID.randomUUID() + "@example.test").query(Long.class).single();
    }
}
