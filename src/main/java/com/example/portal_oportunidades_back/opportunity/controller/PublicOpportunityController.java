package com.example.portal_oportunidades_back.opportunity.controller;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCursorResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.service.OpportunityService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opportunities")
public class PublicOpportunityController {
    private final OpportunityService service;

    public PublicOpportunityController(OpportunityService service) {
        this.service = service;
    }

    @GetMapping
    public OpportunityCursorResponse list(
            @RequestParam(required = false) @Size(max = 1024) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) @Positive Long recruiterId,
            @RequestParam(required = false) @Size(max = 120) String location,
            @RequestParam(required = false) @Size(max = 120) String area,
            @RequestParam(required = false) Instant deadlineFrom,
            @RequestParam(required = false) Instant deadlineTo) {
        return service.listPublic(new OpportunityFilter(q, modality, status, recruiterId,
                location, area, deadlineFrom, deadlineTo), cursor, limit);
    }

    @GetMapping("/{id}")
    public OpportunityResponse get(@PathVariable @Positive Long id) {
        return service.findPublicById(id);
    }
}
