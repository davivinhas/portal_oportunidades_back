package com.example.portal_oportunidades_back.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(schema = "app_auth", name = "administrador")
public class Administrator {
    @Id
    private Long id;
    @MapsId @OneToOne(optional = false) @JoinColumn(name = "usuario_id")
    private User user;
    protected Administrator() { }
}
