package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    List<Opportunity> findAllByRecruiterIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long recruiterId);

    @EntityGraph(attributePaths = "recruiter")
    Optional<Opportunity> findByIdAndDeletedAtIsNull(Long id);
}
