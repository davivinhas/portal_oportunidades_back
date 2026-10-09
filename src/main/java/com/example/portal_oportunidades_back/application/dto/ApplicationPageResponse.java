package com.example.portal_oportunidades_back.application.dto;

import java.util.List;

public record ApplicationPageResponse(List<ApplicationResponse> items, int page, int size,
        long totalElements, int totalPages, boolean hasNext) { }
