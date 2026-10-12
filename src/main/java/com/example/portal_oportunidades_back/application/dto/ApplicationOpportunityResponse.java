package com.example.portal_oportunidades_back.application.dto;

import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import java.time.Instant;

public record ApplicationOpportunityResponse(Long id, String title, OpportunityModality modality,
        String location, OpportunityStatus status, Long recruiterId, String organizationName,
        Instant deletedAt) { }
