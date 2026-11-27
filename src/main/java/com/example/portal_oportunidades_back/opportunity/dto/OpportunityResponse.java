package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record OpportunityResponse(Long id, Long recruiterId, String title, String description,
        String requirements, String modality, String location, String area, Integer vacancyCount,
        Instant registrationStartsAt, Instant registrationEndsAt, OpportunityStatus status,
        Instant createdAt, Instant updatedAt) {
    public OpportunityResponse(Long id, Long recruiterId, String title, String description, String requirements,
            String modality, String location, Integer vacancyCount, Instant registrationStartsAt,
            Instant registrationEndsAt, OpportunityStatus status, Instant createdAt, Instant updatedAt) {
        this(id, recruiterId, title, description, requirements, modality, location, null, vacancyCount,
                registrationStartsAt, registrationEndsAt, status, createdAt, updatedAt);
    }
}
