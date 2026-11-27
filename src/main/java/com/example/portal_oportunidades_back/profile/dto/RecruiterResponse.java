package com.example.portal_oportunidades_back.profile.dto;


import java.time.Instant;

public record RecruiterResponse(
        Long id,
        String name,
        String type,
        String organizationName,
        String description,
        String focusArea,
        String website,
        boolean authorized,
        Instant createdAt,
        Instant updatedAt
) {
    public RecruiterResponse(Long id, String type, String organizationName, boolean authorized,
            Instant createdAt, Instant updatedAt) {
        this(id, null, type, organizationName, null, null, null, authorized, createdAt, updatedAt);
    }
}
