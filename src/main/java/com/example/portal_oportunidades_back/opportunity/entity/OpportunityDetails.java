package com.example.portal_oportunidades_back.opportunity.entity;
import com.example.portal_oportunidades_back.exception.DomainException;

import java.time.Instant;

public record OpportunityDetails(
        String title,
        String description,
        String requirements,
        OpportunityModality modality,
        String location,
        Integer vacancyCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt
) {
    public OpportunityDetails{
        title = requiredText(title, "Title");
        description = requiredText(description, "Description");
        requirements = optionalText(requirements);
        location = optionalText(location);
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.strip();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
