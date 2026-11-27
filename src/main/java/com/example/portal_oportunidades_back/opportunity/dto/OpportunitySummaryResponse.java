package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record OpportunitySummaryResponse(
        Long id,
        String title,
        String modality,
        String location,
        String area,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        OpportunityStatus status,
        Instant createdAt,
        Long recruiterId
) {
}
