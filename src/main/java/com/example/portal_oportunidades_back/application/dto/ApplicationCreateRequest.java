package com.example.portal_oportunidades_back.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ApplicationCreateRequest(@NotNull @Positive Long studentId) { }
