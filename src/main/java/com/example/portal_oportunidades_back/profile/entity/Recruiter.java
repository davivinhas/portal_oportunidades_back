package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "perfil", name = "recrutador")
public class Recruiter {
    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "perfil.tipo_recrutador")
    private RecruiterType tipo;

    @Column(name = "nome_organizacao", nullable = false, length = 200)
    private String organizationName;

    @Column(nullable = false)
    private boolean autorizado;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    protected Recruiter() {
    }

    public Long getId() { return id; }
    public String getOrganizationName() { return organizationName; }
}