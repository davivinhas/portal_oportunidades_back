package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/recruiters/{recruiterId}/opportunities")
@Tag(name = "Recruiter opportunities")
public class OpportunityController {
    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping
    @Operation(summary = "Create an opportunity as a draft")
    public ResponseEntity<OpportunityResponse> create(@PathVariable Long recruiterId,
            @Valid @RequestBody OpportunityCreateRequest request) {
        OpportunityResponse response = opportunityService.create(recruiterId, request);
        URI location = URI.create("/api/recruiters/%d/opportunities/%d".formatted(recruiterId, response.id()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List opportunities owned by a recruiter")
    public ResponseEntity<List<OpportunityResponse>> listOwned(@PathVariable Long recruiterId) {
        return ResponseEntity.ok(opportunityService.listOwned(recruiterId));
    }

    @GetMapping("/{opportunityId}")
    @Operation(summary = "Get an opportunity owned by a recruiter")
    public ResponseEntity<OpportunityResponse> getOwned(@PathVariable Long recruiterId,
            @PathVariable Long opportunityId) {
        return ResponseEntity.ok(opportunityService.getOwned(recruiterId, opportunityId));
    }

    @PutMapping("/{opportunityId}")
    @Operation(summary = "Update a draft opportunity owned by a recruiter")
    public ResponseEntity<OpportunityResponse> update(@PathVariable Long recruiterId,
            @PathVariable Long opportunityId, @Valid @RequestBody OpportunityUpdateRequest request) {
        return ResponseEntity.ok(opportunityService.update(recruiterId, opportunityId, request));
    }

    @PostMapping("/{opportunityId}/publish")
    @Operation(summary = "Publish a draft opportunity")
    public ResponseEntity<OpportunityResponse> publish(@PathVariable Long recruiterId,
            @PathVariable Long opportunityId) {
        return ResponseEntity.ok(opportunityService.publish(recruiterId, opportunityId));
    }

    @PostMapping("/{opportunityId}/close")
    @Operation(summary = "Close a published opportunity")
    public ResponseEntity<OpportunityResponse> close(@PathVariable Long recruiterId,
            @PathVariable Long opportunityId) {
        return ResponseEntity.ok(opportunityService.close(recruiterId, opportunityId));
    }

    @DeleteMapping("/{opportunityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete an opportunity owned by a recruiter")
    public void delete(@PathVariable Long recruiterId, @PathVariable Long opportunityId) {
        opportunityService.delete(recruiterId, opportunityId);
    }
}
