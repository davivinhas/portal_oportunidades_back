package com.example.portal_oportunidades_back.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecruiterUpdateRequest(
        @NotBlank @Size(max = 60) String type,
        @Size(max = 200) String organizationName,
        @Size(max = 5000) String description,
        @Size(max = 120) String focusArea,
        @Size(max = 255) String website
) {
    public RecruiterUpdateRequest(String type, String organizationName) { this(type, organizationName, null, null, null); }
}
