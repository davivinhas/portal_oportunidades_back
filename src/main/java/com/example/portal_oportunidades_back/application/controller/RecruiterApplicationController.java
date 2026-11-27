package com.example.portal_oportunidades_back.application.controller;

import com.example.portal_oportunidades_back.application.dto.ApplicationPageResponse;
import com.example.portal_oportunidades_back.application.dto.ApplicationResponse;
import com.example.portal_oportunidades_back.application.dto.ApplicationStatusRequest;
import com.example.portal_oportunidades_back.application.service.ApplicationService;
import com.example.portal_oportunidades_back.application.service.ApplicationSelectionService;
import com.example.portal_oportunidades_back.application.dto.SelectionStageRequest;
import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recruiters/{recruiterId}/opportunities/{opportunityId}/applications")
@PreAuthorize("hasRole('RECRUITER') and authentication.name == #recruiterId.toString()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Recruiter applications")
public class RecruiterApplicationController {
    private final ApplicationService service;
    private final ApplicationSelectionService selection;

    public RecruiterApplicationController(ApplicationService service, ApplicationSelectionService selection) {
        this.service = service; this.selection = selection;
    }

    @GetMapping("/{applicationId}/status-history")
    @Operation(summary = "Get an application's status change history")
    public java.util.List<com.example.portal_oportunidades_back.application.dto.ApplicationStatusHistoryResponse> history(
            @PathVariable Long recruiterId, @PathVariable Long opportunityId, @PathVariable Long applicationId) {
        return selection.history(recruiterId, opportunityId, applicationId);
    }

    @GetMapping("/{applicationId}/stages")
    @Operation(summary = "List configured selection stages for an application")
    public java.util.List<com.example.portal_oportunidades_back.application.dto.SelectionStageResponse> stages(
            @PathVariable Long recruiterId, @PathVariable Long opportunityId, @PathVariable Long applicationId) {
        return selection.listStages(recruiterId, opportunityId, applicationId);
    }

    @PutMapping("/{applicationId}/stages")
    @Operation(summary = "Replace the configured selection stages for an application")
    public java.util.List<com.example.portal_oportunidades_back.application.dto.SelectionStageResponse> configureStages(
            @PathVariable Long recruiterId, @PathVariable Long opportunityId, @PathVariable Long applicationId,
            @Valid @RequestBody java.util.List<@Valid SelectionStageRequest> request) {
        return selection.configureStages(recruiterId, opportunityId, applicationId, request);
    }

    @GetMapping
    @Operation(summary = "List applications for an owned opportunity",
            description = "Requires an authorized recruiter. Supports a status filter and zero-based pagination (size 1–100).")
    public ApplicationPageResponse list(@PathVariable Long recruiterId, @PathVariable Long opportunityId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.listCandidates(recruiterId, opportunityId, status, page, size);
    }

    @PatchMapping("/{applicationId}/status")
    @Operation(summary = "Update an application's current status",
            description = "Requires an authorized owner. Final statuses cannot change; cancellation belongs to the student.")
    public ApplicationResponse updateStatus(@PathVariable Long recruiterId, @PathVariable Long opportunityId,
            @PathVariable Long applicationId, @Valid @RequestBody ApplicationStatusRequest request) {
        return service.updateStatus(recruiterId, opportunityId, applicationId, request.status());
    }
}
