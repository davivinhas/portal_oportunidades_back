package com.example.portal_oportunidades_back.application.dto;

import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(@NotNull ApplicationStatus status) { }
