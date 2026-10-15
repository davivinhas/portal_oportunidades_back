package com.example.portal_oportunidades_back.opportunity.dto;

import java.util.List;

public record OpportunityCursorResponse(
        List<OpportunitySummaryResponse> items,
        String nextCursor,
        boolean hasNext
) {
    public OpportunityCursorResponse {
        items = List.copyOf(items);
    }
}
