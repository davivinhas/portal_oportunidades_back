package com.example.portal_oportunidades_back.administration.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record CatalogItemRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]*") @Size(max=60) String code,
        @NotBlank @Size(max=120) String label) { }
