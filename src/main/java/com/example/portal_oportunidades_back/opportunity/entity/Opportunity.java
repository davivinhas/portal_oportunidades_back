package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "oportunidades", name = "oportunidade")
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recrutador_id", nullable = false)
    private Recruiter recruiter;

    @Column(name = "titulo", nullable = false, length = 200)
    private String title;

    @Column(name = "descricao", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "requisitos", columnDefinition = "text")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "modalidade", nullable = false, columnDefinition = "oportunidades.modalidade_oportunidade")
    private OpportunityModality modality;

    @Column(name = "localizacao", length = 200)
    private String location;

    @Column(name = "quantidade_vagas", nullable = false)
    private Integer vacancyCount;

    @Column(name = "inicio_inscricoes", nullable = false)
    private Instant registrationStartsAt;

    @Column(name = "fim_inscricoes", nullable = false)
    private Instant registrationEndsAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "oportunidades.status_oportunidade")
    private OpportunityStatus status;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Opportunity() { }

    public Opportunity(Recruiter recruiter, OpportunityDetails details) {
        this.recruiter = Objects.requireNonNull(recruiter, "recruiter must not be null");
        apply(details);
        this.status = OpportunityStatus.DRAFT;
    }

    public void updateDetails(OpportunityDetails details) {
        requireNotDeleted();
        if (status != OpportunityStatus.DRAFT) {
            throw new BusinessException("Only draft opportunities can be edited");
        }
        apply(details);
    }

    public void publish(Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.DRAFT) {
            throw new BusinessException("Only draft opportunities can be published");
        }
        if (!registrationEndsAt.isAfter(now)) {
            throw new BusinessException("Registration must end in the future");
        }
        if (!recruiter.isAuthorized()) {
            throw new BusinessException("Recruiter is not authorized to publish opportunities");
        }
        status = OpportunityStatus.PUBLISHED;
        updatedAt = timestamp(now);
    }

    public void close(Instant now) {
        requireNotDeleted();
        if (status == OpportunityStatus.CLOSED) return;
        if (status != OpportunityStatus.PUBLISHED) {
            throw new BusinessException("Only published opportunities can be closed");
        }
        status = OpportunityStatus.CLOSED;
        updatedAt = timestamp(now);
    }

    public void softDelete(Instant now) {
        requireNotDeleted();
        deletedAt = timestamp(now);
        updatedAt = deletedAt;
    }

    public boolean belongsTo(Long recruiterId) {
        return recruiterId != null && recruiterId.equals(recruiter.getId());
    }

    public boolean isPubliclyAvailable() {
        return deletedAt == null && status == OpportunityStatus.PUBLISHED;
    }

    public boolean canReceiveApplications(Instant at) {
        return isPubliclyAvailable() && !at.isBefore(registrationStartsAt) && !at.isAfter(registrationEndsAt);
    }

    private void apply(OpportunityDetails details) {
        Objects.requireNonNull(details, "details must not be null");
        title = details.title();
        description = details.description();
        requirements = details.requirements();
        modality = details.modality();
        location = details.location();
        vacancyCount = details.vacancyCount();
        registrationStartsAt = details.registrationStartsAt();
        registrationEndsAt = details.registrationEndsAt();
    }

    private void requireNotDeleted() {
        if (deletedAt != null) throw new BusinessException("Deleted opportunities cannot be changed");
    }

    private static Instant timestamp(Instant instant) {
        return Objects.requireNonNull(instant, "current time must not be null").truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Recruiter getRecruiter() { return recruiter; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
    public OpportunityModality getModality() { return modality; }
    public String getLocation() { return location; }
    public Integer getVacancyCount() { return vacancyCount; }
    public Instant getRegistrationStartsAt() { return registrationStartsAt; }
    public Instant getRegistrationEndsAt() { return registrationEndsAt; }
    public OpportunityStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
}
