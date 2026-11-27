package com.example.portal_oportunidades_back.application.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record SelectionStageRequest(@NotBlank @Size(max = 120) String name) { }
