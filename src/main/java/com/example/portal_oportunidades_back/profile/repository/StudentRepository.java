package com.example.portal_oportunidades_back.profile.repository;

import com.example.portal_oportunidades_back.profile.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> { }
