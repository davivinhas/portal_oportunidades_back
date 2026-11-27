package com.example.portal_oportunidades_back.administration.controller;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import java.time.Clock;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/opportunities")
@PreAuthorize("hasRole('ADMIN')")
public class OpportunityModerationController {
    private final OpportunityRepository opportunities;
    private final OpportunityMapper mapper;
    private final Clock clock;
    public OpportunityModerationController(OpportunityRepository opportunities, OpportunityMapper mapper, Clock clock) {
        this.opportunities = opportunities; this.mapper = mapper; this.clock = clock;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<OpportunityResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new com.example.portal_oportunidades_back.exception.BadRequestException("Invalid pagination");
        return opportunities.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(mapper::toResponse);
    }

    @PutMapping("/{opportunityId}/moderation/close")
    @Transactional
    public ResponseEntity<Void> close(@PathVariable Long opportunityId) {
        var opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new com.example.portal_oportunidades_back.exception.ResourceNotFoundException("Opportunity not found: " + opportunityId));
        opportunity.close(clock.instant());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{opportunityId}/moderation")
    @Transactional
    public ResponseEntity<Void> remove(@PathVariable Long opportunityId) {
        var opportunity = opportunities.findById(opportunityId)
                .orElseThrow(() -> new com.example.portal_oportunidades_back.exception.ResourceNotFoundException("Opportunity not found: " + opportunityId));
        opportunity.softDelete(clock.instant());
        return ResponseEntity.noContent().build();
    }
}
