package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    List<Opportunity> findAllByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);
}
