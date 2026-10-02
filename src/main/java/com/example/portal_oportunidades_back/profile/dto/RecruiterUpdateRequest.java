package com.example.portal_oportunidades_back.profile.dto;

import com.example.portal_oportunidades_back.profile.entity.RecruiterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecruiterUpdateRequest(
        @NotNull RecruiterType type,
        @NotBlank @Size(max = 200) String organizationName
) { }
