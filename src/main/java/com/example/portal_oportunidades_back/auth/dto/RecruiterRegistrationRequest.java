package com.example.portal_oportunidades_back.auth.dto;
import com.example.portal_oportunidades_back.profile.entity.RecruiterType;
import jakarta.validation.constraints.*;
public record RecruiterRegistrationRequest(@NotBlank @Size(max=150) String name,
        @NotBlank @Email @Size(max=255) String email, @NotBlank @Size(min=8, max=72) String password,
        @NotNull RecruiterType type, @NotBlank @Size(max=200) String organizationName) { }
