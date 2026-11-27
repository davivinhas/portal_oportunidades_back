package com.example.portal_oportunidades_back.profile.repository;

import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "user")
    org.springframework.data.domain.Page<Recruiter> findAllByAuthorizedFalse(org.springframework.data.domain.Pageable pageable);
}
