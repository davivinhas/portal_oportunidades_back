package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.auth.entity.Administrator;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(schema = "oportunidades", name = "oportunidade")
public class Opportunity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recrutador_id", nullable = false) private Recruiter recruiter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "avaliador_id") private Administrator evaluator;
    @Column(nullable = false, length = 200) private String titulo;
    @Column(nullable = false) private String descricao;
    private String requisitos;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.modalidade_oportunidade") private OpportunityModality modalidade;
    @Column(length = 200) private String localizacao;
    @Column(name = "quantidade_vagas", nullable = false) private Integer vacancyCount;
    @Column(name = "inicio_inscricoes", nullable = false) private Instant registrationStartsAt;
    @Column(name = "fim_inscricoes", nullable = false) private Instant registrationEndsAt;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "oportunidades.status_oportunidade") private OpportunityStatus status;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false) private Instant updatedAt;
    protected Opportunity() { }
}
