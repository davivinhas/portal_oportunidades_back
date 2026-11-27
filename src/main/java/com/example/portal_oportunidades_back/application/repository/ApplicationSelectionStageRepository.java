package com.example.portal_oportunidades_back.application.repository;

import com.example.portal_oportunidades_back.opportunity.entity.ApplicationSelectionStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSelectionStageRepository extends JpaRepository<ApplicationSelectionStage, Long> {
    List<ApplicationSelectionStage> findAllByApplicationIdOrderByOrderAsc(Long applicationId);
    void deleteAllByApplicationId(Long applicationId);
}
