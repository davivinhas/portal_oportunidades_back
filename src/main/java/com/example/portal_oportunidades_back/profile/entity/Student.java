package com.example.portal_oportunidades_back.profile.entity;

import com.example.portal_oportunidades_back.auth.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.Objects;
import com.example.portal_oportunidades_back.exception.BusinessException;

@Entity
@Table(schema = "perfil", name = "aluno")
public class Student {
    @Id private Long id;
    @MapsId @OneToOne(optional = false) @JoinColumn(name = "usuario_id")
    private User user;
    @Column(name = "matricula", nullable = false, length = 30)
    private String registrationNumber;
    @Column(name = "curso", nullable = false, length = 150)
    private String course;
    @Column(name = "periodo")
    private Short semester;
    @Column(name = "telefone", length = 30)
    private String phone;
    @Column(name = "resumo", columnDefinition = "text")
    private String summary;
    @Column(name = "habilidades", columnDefinition = "text")
    private String skills;
    @Column(name = "interesses", columnDefinition = "text")
    private String interests;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private Instant updatedAt;
    protected Student() { }

    public Student(User user, String registrationNumber, String course) {
        this.user = Objects.requireNonNull(user);
        updateProfile(registrationNumber, course, null, null, null, null, null);
    }

    public void updateProfile(String registrationNumber, String course, Short semester, String phone,
                              String summary, String skills, String interests) {
        if (registrationNumber == null || registrationNumber.isBlank() || registrationNumber.trim().length() > 30)
            throw new BusinessException("Registration number must contain between 1 and 30 characters");
        if (course == null || course.isBlank() || course.trim().length() > 150)
            throw new BusinessException("Course must contain between 1 and 150 characters");
        if (semester != null && semester < 1) throw new BusinessException("Semester must be positive");
        if (phone != null && phone.length() > 30) throw new BusinessException("Phone must not exceed 30 characters");
        this.registrationNumber = registrationNumber.trim();
        this.course = course.trim();
        this.semester = semester;
        this.phone = normalize(phone);
        this.summary = normalize(summary);
        this.skills = normalize(skills);
        this.interests = normalize(interests);
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getCourse() { return course; }
    public Short getSemester() { return semester; }
    public String getPhone() { return phone; }
    public String getSummary() { return summary; }
    public String getSkills() { return skills; }
    public String getInterests() { return interests; }
}
