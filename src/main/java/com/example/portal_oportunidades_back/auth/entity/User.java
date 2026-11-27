package com.example.portal_oportunidades_back.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(schema = "app_auth", name = "usuario")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false)
    private boolean active = true;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;
    protected User() { }

    public User(String name, String email, String passwordHash) {
        this.name = name.trim();
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public void suspend() { active = false; }
    public void reactivate() { active = true; }
}
