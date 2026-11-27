package com.example.portal_oportunidades_back.application.repository;

import com.example.portal_oportunidades_back.opportunity.entity.Application;
import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentIdAndOpportunityId(Long studentId, Long opportunityId);
    boolean existsByIdAndOpportunityId(Long id, Long opportunityId);

    @EntityGraph(attributePaths = {"opportunity", "opportunity.recruiter"})
    Page<Application> findAllByStudentId(Long studentId, Pageable pageable);

    @EntityGraph(attributePaths = {"opportunity", "opportunity.recruiter"})
    Page<Application> findAllByStudentIdAndStatus(Long studentId, ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "opportunity", "opportunity.recruiter"})
    Optional<Application> findByIdAndStudentId(Long id, Long studentId);

    @EntityGraph(attributePaths = {"student", "opportunity", "opportunity.recruiter"})
    Page<Application> findAllByOpportunityId(Long opportunityId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "opportunity", "opportunity.recruiter"})
    Page<Application> findAllByOpportunityIdAndStatus(Long opportunityId, ApplicationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "opportunity", "opportunity.recruiter"})
    Optional<Application> findByIdAndOpportunityId(Long id, Long opportunityId);
}
