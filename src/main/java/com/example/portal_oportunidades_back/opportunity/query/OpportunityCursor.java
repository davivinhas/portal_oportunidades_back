package com.example.portal_oportunidades_back.opportunity.query;

import java.time.Instant;

public record OpportunityCursor(Double rank, Instant createdAt, long id) {
    public OpportunityCursor(Instant createdAt, long id) {
        this(null, createdAt, id);
    }
}
