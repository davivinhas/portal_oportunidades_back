package com.example.portal_oportunidades_back.administration.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CatalogItemUpdateRequest(@NotBlank @Size(max=120) String label, Boolean active) { }
