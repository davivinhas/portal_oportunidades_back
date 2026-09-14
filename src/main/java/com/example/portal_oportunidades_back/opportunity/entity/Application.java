package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.profile.entity.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(schema = "oportunidades", name = "candidatura")
public class Application {
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
}
