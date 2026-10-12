package com.example.portal_oportunidades_back.application.dto;

import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import java.time.Instant;

public record ApplicationResponse(Long id, Long studentId, ApplicationOpportunityResponse opportunity,
        ApplicationStatus status, Instant appliedAt, Instant createdAt, Instant updatedAt) { }
