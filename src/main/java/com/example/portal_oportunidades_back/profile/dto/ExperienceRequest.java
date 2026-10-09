package com.example.portal_oportunidades_back.profile.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ExperienceRequest(
        @Positive Long id,
        @NotBlank @Size(max = 150) String position,
        @NotBlank @Size(max = 200) String organization,
        @NotNull @PastOrPresent LocalDate startDate,
        @PastOrPresent LocalDate endDate,
        boolean current,
        @Size(max = 10000) String description) { }
