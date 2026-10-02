package com.example.portal_oportunidades_back.opportunity.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record OpportunityUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotNull OpportunityModality modality,
        @Size(max = 200) String location,
        @NotNull @Positive Integer vacancyCount,
        @NotNull @Future Instant registrationStartsAt,
        @NotNull @Future Instant registrationEndsAt
) { }
