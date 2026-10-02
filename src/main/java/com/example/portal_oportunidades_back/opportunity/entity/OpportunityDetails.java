package com.example.portal_oportunidades_back.opportunity.entity;
import com.example.portal_oportunidades_back.exception.BadRequestException;
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
    public OpportunityDetails {
        title = requiredText(title, "Title");
        description = requiredText(description, "Description");
        requirements = optionalText(requirements);
        location = optionalText(location);
        if (title.length() > 200) {
            throw new BadRequestException("Title must have at most 200 characters");
        }
        if (location != null && location.length() > 200) {
            throw new BadRequestException("Location must have at most 200 characters");
        }
        if (modality == null) {
            throw new BadRequestException("Modality is required");
        }
        if (vacancyCount == null || vacancyCount < 1) {
            throw new BadRequestException("Vacancy count must be greater than zero");
        }
        if (registrationStartsAt == null || registrationEndsAt == null
                || !registrationStartsAt.isBefore(registrationEndsAt)) {
            throw new BadRequestException("Registration start must be before registration end");
        }
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        return value.strip();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
