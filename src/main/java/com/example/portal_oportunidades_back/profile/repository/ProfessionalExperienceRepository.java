package com.example.portal_oportunidades_back.profile.repository;

import com.example.portal_oportunidades_back.profile.entity.ProfessionalExperience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalExperienceRepository extends JpaRepository<ProfessionalExperience, Long> {
    List<ProfessionalExperience> findAllByStudentIdOrderByStartDateDescIdAsc(Long studentId);
}
