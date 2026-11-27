package com.example.portal_oportunidades_back.opportunity.entity;

import jakarta.persistence.*;

@Entity
@Table(schema = "oportunidades", name = "etapa_seletiva_candidatura",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_stage_order", columnNames = {"candidatura_id", "ordem"}))
public class ApplicationSelectionStage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "candidatura_id", nullable = false)
    private Application application;
    @Column(name = "nome", nullable = false, length = 120) private String name;
    @Column(name = "ordem", nullable = false) private int order;
    protected ApplicationSelectionStage() { }
    public ApplicationSelectionStage(Application application, String name, int order) {
        this.application = application;
        if (name == null || name.isBlank() || name.trim().length() > 120) throw new IllegalArgumentException("Stage name is required and must not exceed 120 characters");
        this.name = name.trim(); this.order = order;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getOrder() { return order; }
}
