package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(schema = "perfil", name = "aluno")
public class Student {
    @Id private Long id;
    @MapsId @OneToOne(optional = false) @JoinColumn(name = "usuario_id")
    private User user;
    @Column(nullable = false, length = 30)
    private String matricula;
    @Column(nullable = false, length = 150)
    private String curso;
    private Short periodo;
    @Column(length = 30)
    private String telefone;
    private String resumo;
    private String habilidades;
    private String interesses;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;
    protected Student() { }
}
