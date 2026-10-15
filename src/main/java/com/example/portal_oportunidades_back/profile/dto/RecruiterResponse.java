package com.example.portal_oportunidades_back.profile.dto;

import com.example.portal_oportunidades_back.profile.entity.RecruiterType;

import java.time.Instant;

public record RecruiterResponse(
        Long id,
        RecruiterType type,
        String organizationName,
        boolean authorized,
        Instant createdAt,
        Instant updatedAt
) { }
