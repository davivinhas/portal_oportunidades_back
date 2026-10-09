package com.example.portal_oportunidades_back.profile.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;
import com.example.portal_oportunidades_back.exception.BusinessException;
import java.util.Objects;

@Entity
@Table(schema = "perfil", name = "experiencia_profissional")
public class ProfessionalExperience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "aluno_id", nullable = false)
    private Student student;
    @Column(name = "cargo", nullable = false, length = 150)
    private String position;
    @Column(name = "organizacao", nullable = false, length = 200)
    private String organization;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate startDate;
    @Column(name = "data_fim")
    private LocalDate endDate;
    @Column(name = "atual", nullable = false)
    private boolean current;
    @Column(name = "descricao", columnDefinition = "text")
    private String description;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;
    protected ProfessionalExperience() { }

    public ProfessionalExperience(Student student, String position, String organization,
            LocalDate startDate, LocalDate endDate, boolean current, String description, LocalDate today) {
        this.student = Objects.requireNonNull(student);
        update(position, organization, startDate, endDate, current, description, today);
    }

    public void update(String position, String organization, LocalDate startDate, LocalDate endDate,
                       boolean current, String description, LocalDate today) {
        if (position == null || position.isBlank() || position.trim().length() > 150)
            throw new BusinessException("Position must contain between 1 and 150 characters");
        if (organization == null || organization.isBlank() || organization.trim().length() > 200)
            throw new BusinessException("Organization must contain between 1 and 200 characters");
        if (startDate == null || startDate.isAfter(today)) throw new BusinessException("Start date must not be in the future");
        if (current && endDate != null) throw new BusinessException("Current experience cannot have an end date");
        if (!current && endDate == null) throw new BusinessException("Finished experience requires an end date");
        if (endDate != null && (endDate.isBefore(startDate) || endDate.isAfter(today)))
            throw new BusinessException("End date must be between start date and today");
        this.position = position.trim();
        this.organization = organization.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.current = current;
        this.description = description == null || description.isBlank() ? null : description.trim();
    }
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public String getPosition() { return position; }
    public String getOrganization() { return organization; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCurrent() { return current; }
    public String getDescription() { return description; }
}
