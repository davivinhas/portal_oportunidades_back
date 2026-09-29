package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.auth.entity.Administrator;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(schema = "oportunidades", name = "oportunidade")
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recrutador_id", nullable = false)
    private Recruiter recruiter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliador_id")
    private Administrator evaluator;
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

    protected Opportunity() { }

    public Opportunity(Recruiter recruiter, String title, String description, String requirements,
                       OpportunityModality modality, String location, Integer vacancyCount,
                       Instant registrationStartsAt, Instant registrationEndsAt) {
        this.recruiter = Objects.requireNonNull(recruiter, "recruiter must not be null");
        applyEditableFields(title, description, requirements, modality, location, vacancyCount,
                registrationStartsAt, registrationEndsAt);
        this.status = OpportunityStatus.DRAFT;
    }

    public void updateDetails(String title, String description, String requirements,
                              OpportunityModality modality, String location, Integer vacancyCount,
                              Instant registrationStartsAt, Instant registrationEndsAt) {
        if (status != OpportunityStatus.DRAFT) {
            throw new BusinessException("Only draft opportunities can be edited");
        }
        applyEditableFields(title, description, requirements, modality, location, vacancyCount,
                registrationStartsAt, registrationEndsAt);
    }

    public void publish() {
        if (status != OpportunityStatus.DRAFT) {
            throw new BusinessException("Only draft opportunities can be published");
        }
        if (!recruiter.isAuthorized()) {
            throw new BusinessException("Recruiter is not authorized to publish opportunities");
        }
        status = OpportunityStatus.PUBLISHED;
    }

    public void close() {
        if (status == OpportunityStatus.CLOSED) {
            return;
        }
        if (status != OpportunityStatus.PUBLISHED) {
            throw new BusinessException("Only published opportunities can be closed");
        }
        status = OpportunityStatus.CLOSED;
    }

    public boolean canReceiveApplications(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        return status == OpportunityStatus.PUBLISHED
                && !at.isBefore(registrationStartsAt)
                && !at.isAfter(registrationEndsAt);
    }

    public boolean belongsTo(Long recruiterId) {
        return recruiterId != null && recruiterId.equals(recruiter.getId());
    }

    private void applyEditableFields(String title, String description, String requirements,
                                     OpportunityModality modality, String location, Integer vacancyCount,
                                     Instant registrationStartsAt, Instant registrationEndsAt) {
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.requirements = trimToNull(requirements);
        this.modality = Objects.requireNonNull(modality, "modality must not be null");
        this.location = trimToNull(location);
        if (vacancyCount == null || vacancyCount <= 0) {
            throw new BusinessException("Vacancy count must be greater than zero");
        }
        this.vacancyCount = vacancyCount;
        this.registrationStartsAt = Objects.requireNonNull(registrationStartsAt,
                "registrationStartsAt must not be null");
        this.registrationEndsAt = Objects.requireNonNull(registrationEndsAt,
                "registrationEndsAt must not be null");
        if (!registrationEndsAt.isAfter(registrationStartsAt)) {
            throw new BusinessException("Registration end must be after registration start");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Recruiter getRecruiter() { return recruiter; }
    public Administrator getEvaluator() { return evaluator; }
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
}
