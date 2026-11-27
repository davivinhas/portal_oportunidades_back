package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.util.Locale;
import java.time.Instant;

public record OpportunityFilter(
        String query,
        String modality,
        OpportunityStatus status,
        Long recruiterId,
        String location,
        String area,
        Instant deadlineFrom,
        Instant deadlineTo
) {
    public OpportunityFilter(String query, String modality,
            com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus status, Long recruiterId) {
        this(query, modality, status, recruiterId, null, null, null, null);
    }
    public OpportunityFilter {
        query = query == null || query.isBlank() ? null : query.strip().toLowerCase(Locale.ROOT);
        if (query != null && query.length() > 200) {
            throw new BadRequestException("Query filter must have at most 200 characters");
        }
        if (recruiterId != null && recruiterId < 1) {
            throw new BadRequestException("Recruiter ID must be positive");
        }
        location = normalize(location, "Location");
        area = normalize(area, "Area");
        if (deadlineFrom != null && deadlineTo != null && deadlineFrom.isAfter(deadlineTo))
            throw new BadRequestException("Deadline start must not be after deadline end");
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 120) throw new BadRequestException(field + " filter must have at most 120 characters");
        return normalized;
    }
}
