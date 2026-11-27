package com.example.portal_oportunidades_back.auth.dto;
import jakarta.validation.constraints.*;
public record RecruiterRegistrationRequest(@NotBlank @Size(max=150) String name,
        @NotBlank @Email @Size(max=255) String email, @NotBlank @Size(min=8, max=72) String password,
        @NotBlank @Size(max=60) String type, @Size(max=200) String organizationName) { }
