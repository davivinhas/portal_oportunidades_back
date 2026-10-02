package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
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
    @Column(name = "tipo", nullable = false, columnDefinition = "perfil.tipo_recrutador")
    private RecruiterType type;

    @Column(name = "nome_organizacao", nullable = false, length = 200)
    private String organizationName;

    @Column(name = "autorizado", nullable = false)
    private boolean authorized;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    protected Recruiter() { }

    public Recruiter(User user, RecruiterType type, String organizationName, boolean authorized) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.organizationName = requireText(organizationName, "organizationName");
        this.authorized = authorized;
    }

    public void updateProfile(RecruiterType type, String organizationName) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.organizationName = requireText(organizationName, "organizationName");
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public RecruiterType getType() { return type; }
    public String getOrganizationName() { return organizationName; }
    public boolean isAuthorized() { return authorized; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
