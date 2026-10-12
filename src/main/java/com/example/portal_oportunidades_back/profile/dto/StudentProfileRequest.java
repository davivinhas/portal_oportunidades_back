package com.example.portal_oportunidades_back.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record StudentProfileRequest(
        @NotBlank @Size(max = 30) String registrationNumber,
        @NotBlank @Size(max = 150) String course,
        @Positive Short semester,
        @Size(max = 30) String phone,
        @Size(max = 10000) String summary,
        @Size(max = 10000) String skills,
        @Size(max = 10000) String interests,
        @NotNull @Size(max = 100) List<@NotNull @Valid ExperienceRequest> experiences) { }
