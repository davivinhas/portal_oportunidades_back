package com.example.portal_oportunidades_back.opportunity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(schema = "oportunidades", name = "historico_status_candidatura")
public class ApplicationStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Application application;
    @Enumerated(EnumType.STRING) @Column(name = "status_anterior", length = 30)
    private ApplicationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "status_novo", nullable = false, length = 30)
    private ApplicationStatus newStatus;
    @CreationTimestamp @Column(name = "alterado_em", nullable = false, updatable = false)
    private Instant changedAt;
    protected ApplicationStatusHistory() { }
    public ApplicationStatusHistory(Application application, ApplicationStatus previousStatus, ApplicationStatus newStatus) {
        this.application = application; this.previousStatus = previousStatus; this.newStatus = newStatus;
    }
    public Long getId() { return id; }
    public ApplicationStatus getPreviousStatus() { return previousStatus; }
    public ApplicationStatus getNewStatus() { return newStatus; }
    public Instant getChangedAt() { return changedAt; }
}
