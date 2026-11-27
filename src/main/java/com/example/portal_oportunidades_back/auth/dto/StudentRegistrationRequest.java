package com.example.portal_oportunidades_back.auth.dto;
import jakarta.validation.constraints.*;
public record StudentRegistrationRequest(@NotBlank @Size(max=150) String name,
        @NotBlank @Email @Size(max=255) String email, @NotBlank @Size(min=8, max=72) String password,
        @NotBlank @Size(max=30) String registrationNumber, @NotBlank @Size(max=150) String course) { }
