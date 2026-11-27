package com.example.portal_oportunidades_back.opportunity.repository;

import java.time.Instant;

public record OpportunitySearchHit(
        Long id,
        Instant createdAt,
        double rank
) {}
