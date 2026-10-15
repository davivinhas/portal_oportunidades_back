package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.util.Locale;

public record OpportunityFilter(
        String title,
        OpportunityModality modality,
        OpportunityStatus status,
        Long recruiterId
) {
    public OpportunityFilter {
        title = title == null || title.isBlank() ? null : title.strip().toLowerCase(Locale.ROOT);
        if (title != null && title.length() > 200) {
            throw new BadRequestException("Title filter must have at most 200 characters");
        }
        if (recruiterId != null && recruiterId < 1) {
            throw new BadRequestException("Recruiter ID must be positive");
        }
    }
}
