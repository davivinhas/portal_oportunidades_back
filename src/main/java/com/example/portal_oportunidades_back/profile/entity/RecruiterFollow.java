package com.example.portal_oportunidades_back.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "perfil", name = "recruiter_follow", uniqueConstraints =
        @UniqueConstraint(name = "uk_recruiter_follow_pair", columnNames = {"student_id", "recruiter_id"}))
public class RecruiterFollow {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecruiterFollow() { }
}
