package com.example.portal_oportunidades_back.profile.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(schema = "perfil", name = "experiencia_profissional")
public class ProfessionalExperience {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "aluno_id", nullable = false)
    private Student student;
    @Column(nullable = false, length = 150)
    private String cargo;
    @Column(nullable = false, length = 200)
    private String organizacao;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate startDate;
    @Column(name = "data_fim")
    private LocalDate endDate;
    @Column(nullable = false)
    private boolean atual;
    private String descricao;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;
    protected ProfessionalExperience() { }
}
