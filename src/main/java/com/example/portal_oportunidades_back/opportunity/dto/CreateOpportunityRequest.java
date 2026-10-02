package com.example.portal_oportunidades_back.opportunity.dto;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateOpportunityRequest(
        @NotNull @Positive Long recruiterId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        String requirements,
        @NotNull OpportunityModality modality,
        @Size(max = 200) String location,
        @NotNull @Positive Integer vacancyCount,
        @NotNull Instant registrationStartsAt,
        @NotNull Instant registrationEndsAt
) {
}
