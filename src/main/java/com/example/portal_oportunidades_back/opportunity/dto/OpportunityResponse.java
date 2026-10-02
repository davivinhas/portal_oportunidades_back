package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record OpportunityResponse(
        Long id,
        String title,
        String description,
        String requirements,
        OpportunityModality modality,
        String location,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        OpportunityStatus status,
        Instant createdAt,
        Instant updatedAt,
        RecruiterSummaryResponse recruiter
) {
}