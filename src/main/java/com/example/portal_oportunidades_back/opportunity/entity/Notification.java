package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(schema = "oportunidades", name = "notificacao")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private User user;
    @Column(nullable = false, length = 200) private String titulo;
    @Column(nullable = false) private String mensagem;
    @Column(name = "data_envio", nullable = false) private Instant sentAt;
    @Column(nullable = false) private boolean lida;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false) private Instant updatedAt;
    protected Notification() { }
}
