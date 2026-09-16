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
import java.time.temporal.ChronoUnit;
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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String description;
    private String requirements;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.modalidade_oportunidade")
    private OpportunityModality modality;

    @Column(length = 200)
    private String location;

    @Column(name = "quantidade_vagas", nullable = false)
    private Integer vacancyCount;

    @Column(name = "inicio_inscricoes", nullable = false)
    private Instant registrationStartsAt;

    @Column(name = "fim_inscricoes", nullable = false)
    private Instant registrationEndsAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.status_oportunidade")
    private OpportunityStatus status;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Opportunity() {
    }

    // Regras de negocio
    public static Opportunity create(Recruiter recruiter, OpportunityDetails details, Instant now) {
        Opportunity opportunity = new Opportunity();
        opportunity.recruiter = Objects.requireNonNull(recruiter, "Recruiter is required");
        opportunity.applyDetails(Objects.requireNonNull(details, "details is required"));
        opportunity.status = OpportunityStatus.RASCUNHO;
        opportunity.createdAt = now;
        opportunity.updatedAt = now;
        return opportunity;
    }

    public void updateDetails(OpportunityDetails details, Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.RASCUNHO && status != OpportunityStatus.PUBLICADA) {
            throw new BusinessException("Only draft or published opportunities can be edited");
        }
        Objects.requireNonNull(details, "Details are required");
        if (status == OpportunityStatus.PUBLICADA) {
            requireFutureEnd(details.registrationEndsAt(), now);
        }
        applyDetails(details);
        updatedAt = timestamp(now);
    }

    public void publish(Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.RASCUNHO) {
            throw new BusinessException("Only draft opportunities can be published");
        }
        requireFutureEnd(registrationEndsAt, now);
        status = OpportunityStatus.PUBLICADA;
        updatedAt = timestamp(now);
    }

    public void close(Instant now) {
        requireNotDeleted();
        if (status != OpportunityStatus.PUBLICADA) {
            throw new BusinessException("Only published opportunities can be closed");
        }
        status = OpportunityStatus.ENCERRADA;
        updatedAt = timestamp(now);
    }

    public boolean canReceiveApplications(Instant now) {
        return deletedAt == null
                && status == OpportunityStatus.PUBLICADA
                && !now.isBefore(registrationStartsAt)
                && now.isBefore(registrationEndsAt);
    }

    // Funções auxiliares
    private void applyDetails(OpportunityDetails details) {
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
        if (deletedAt != null) {
            throw new BusinessException("Deleted opportunities cannot be changed");
        }
    }

    private static void requireFutureEnd(Instant end, Instant now) {
        if (!end.isAfter(now)) {
            throw new BusinessException("Registration must end in the future");
        }
    }

    private static Instant timestamp(Instant now) {
        return Objects.requireNonNull(now, "Current time is required").truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {return id;}

    public Recruiter getRecruiter() {return recruiter;}

    public String getTitle() {return title;}

    public String getDescription() {return description;}

    public String getRequirements() {return requirements;}

    public OpportunityModality getModality() {return modality;}

    public String getLocation() {return location;}

    public Integer getVacancyCount() {return vacancyCount;}

    public Instant getRegistrationStartsAt() {return registrationStartsAt;}

    public Instant getRegistrationEndsAt() {return registrationEndsAt;}

    public OpportunityStatus getStatus() {return status;}

    public Instant getCreatedAt() {return createdAt;}

    public Instant getUpdatedAt() {return updatedAt;}

    public Instant getDeletedAt() {return deletedAt;}
}
