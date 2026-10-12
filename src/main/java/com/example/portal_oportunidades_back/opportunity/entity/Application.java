package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.profile.entity.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;
import com.example.portal_oportunidades_back.exception.BusinessException;

@Entity
@Table(schema = "oportunidades", name = "candidatura", uniqueConstraints =
        @UniqueConstraint(name = "uk_application_student_opportunity", columnNames = {"aluno_id", "oportunidade_id"}))
public class Application {
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "aluno_id", nullable = false) private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "oportunidade_id", nullable = false) private Opportunity opportunity;
    @Column(name = "data_candidatura", nullable = false) private Instant appliedAt;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.status_candidatura") private ApplicationStatus status;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "pontuacao_compatibilidade", precision = 5, scale = 2) private java.math.BigDecimal compatibilityScore;

    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false) private Instant updatedAt;
    protected Application() { }

    public Application(Student student, Opportunity opportunity, Instant appliedAt) {
        this.student = Objects.requireNonNull(student, "student must not be null");
        this.opportunity = Objects.requireNonNull(opportunity, "opportunity must not be null");
        this.appliedAt = Objects.requireNonNull(appliedAt, "appliedAt must not be null");
        if (!opportunity.canReceiveApplications(appliedAt))
            throw new BusinessException("Opportunity is not receiving applications");
        this.status = ApplicationStatus.SUBMITTED;
    }

    public void updateStatus(ApplicationStatus next) {
        if (next == null) throw new BusinessException("Application status is required");
        if (next == status) return;
        if (next == ApplicationStatus.CANCELLED)
            throw new BusinessException("Use cancellation to cancel an application");
        if (isTerminal() || next == ApplicationStatus.SUBMITTED)
            throw new BusinessException("Invalid application status transition");
        status = next;
    }

    public void cancel() {
        if (status == ApplicationStatus.CANCELLED) return;
        if (isTerminal()) throw new BusinessException("Finalized applications cannot be cancelled");
        status = ApplicationStatus.CANCELLED;
    }

    private boolean isTerminal() {
        return status == ApplicationStatus.APPROVED || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Opportunity getOpportunity() { return opportunity; }
    public Instant getAppliedAt() { return appliedAt; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
