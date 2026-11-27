package com.example.portal_oportunidades_back.application.service;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.application.repository.*;
import com.example.portal_oportunidades_back.exception.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('RECRUITER')")
public class ApplicationSelectionService {
    private final ApplicationRepository applications;
    private final ApplicationSelectionStageRepository stages;
    private final OpportunityRepository opportunities;
    public ApplicationSelectionService(ApplicationRepository applications, ApplicationSelectionStageRepository stages,
            OpportunityRepository opportunities) {
        this.applications = applications; this.stages = stages; this.opportunities = opportunities;
    }
    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> history(Long recruiterId, Long opportunityId, Long applicationId) {
        verifyOwner(recruiterId, opportunityId);
        var app = applications.findByIdAndOpportunityId(applicationId, opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return app.getStatusHistory().stream().map(h -> new ApplicationStatusHistoryResponse(
                h.getId(), h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt())).toList();
    }
    @Transactional(readOnly = true)
    public List<SelectionStageResponse> listStages(Long recruiterId, Long opportunityId, Long applicationId) {
        verifyOwner(recruiterId, opportunityId);
        if (!applications.existsByIdAndOpportunityId(applicationId, opportunityId)) throw new ResourceNotFoundException("Application not found");
        return stages.findAllByApplicationIdOrderByOrderAsc(applicationId).stream()
                .map(s -> new SelectionStageResponse(s.getId(), s.getName(), s.getOrder())).toList();
    }
    @Transactional
    public List<SelectionStageResponse> configureStages(Long recruiterId, Long opportunityId, Long applicationId,
            List<SelectionStageRequest> request) {
        verifyOwner(recruiterId, opportunityId);
        Application app = applications.findByIdAndOpportunityId(applicationId, opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (request == null || request.size() > 30) throw new BadRequestException("Provide between 0 and 30 stages");
        stages.deleteAllByApplicationId(applicationId);
        var saved = stages.saveAll(java.util.stream.IntStream.range(0, request.size())
                .mapToObj(i -> new ApplicationSelectionStage(app, request.get(i).name(), i + 1)).toList());
        return saved.stream().map(s -> new SelectionStageResponse(s.getId(), s.getName(), s.getOrder())).toList();
    }
    private void verifyOwner(Long recruiterId, Long opportunityId) {
        var opportunity = opportunities.findByIdAndDeletedAtIsNull(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        if (!opportunity.belongsTo(recruiterId) || !opportunity.getRecruiter().isAuthorized())
            throw new ForbiddenOperationException("Recruiter is not authorized to manage this opportunity");
    }
}
