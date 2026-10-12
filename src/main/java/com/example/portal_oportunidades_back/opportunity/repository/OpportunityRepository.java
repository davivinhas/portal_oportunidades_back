package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Opportunity o where o.id = :id")
    Optional<Opportunity> findByIdForApplication(@org.springframework.data.repository.query.Param("id") Long id);

    List<Opportunity> findAllByRecruiterIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long recruiterId);

    @EntityGraph(attributePaths = "recruiter")
    Optional<Opportunity> findByIdAndDeletedAtIsNull(Long id);
}
