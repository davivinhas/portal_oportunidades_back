package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record OpportunityCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotBlank @Size(max = 60) String modality,
        @Size(max = 200) String location,
        @Size(max = 120) String area,
        @NotNull @Positive Integer vacancyCount,
        @NotNull Instant registrationStartsAt,
        @NotNull @Future Instant registrationEndsAt
) {
    public OpportunityCreateRequest(String title, String description, String requirements, String modality,
            String location, Integer vacancyCount, Instant registrationStartsAt, Instant registrationEndsAt) {
        this(title, description, requirements, modality, location, null, vacancyCount, registrationStartsAt, registrationEndsAt);
    }
}
