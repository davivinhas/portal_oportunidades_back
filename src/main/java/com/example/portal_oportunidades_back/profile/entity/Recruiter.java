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

    @Column(name = "tipo", nullable = false, length = 60)
    private String type;

    @Column(name = "nome_organizacao", length = 200)
    private String organizationName;

    @Column(name = "descricao", columnDefinition = "text") private String description;
    @Column(name = "area_atuacao", length = 120) private String focusArea;
    @Column(name = "site", length = 255) private String website;

    @Column(name = "autorizado", nullable = false)
    private boolean authorized;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    protected Recruiter() { }

    public Recruiter(User user, String type, String organizationName, boolean authorized) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.organizationName = normalizeOrganization(type, organizationName);
        this.authorized = authorized;
    }

    public void updateProfile(String type, String organizationName, String description, String focusArea, String website) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.organizationName = normalizeOrganization(type, organizationName);
        this.description = optionalText(description, 5000, "description");
        this.focusArea = optionalText(focusArea, 120, "focusArea");
        this.website = optionalText(website, 255, "website");
    }

    public void updateProfile(String type, String organizationName) {
        updateProfile(type, organizationName, description, focusArea, website);
    }

    public void authorize() {
        this.authorized = true;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getType() { return type; }
    public String getOrganizationName() { return organizationName; }
    public String getDescription() { return description; }
    public String getFocusArea() { return focusArea; }
    public String getWebsite() { return website; }
    public boolean isAuthorized() { return authorized; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String normalizeOrganization(String type, String value) {
        if (RecruiterType.PROFESSOR.equals(type) && (value == null || value.isBlank())) return null;
        return requireText(value, "organizationName");
    }

    private static String optionalText(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        return value.trim();
    }
}
