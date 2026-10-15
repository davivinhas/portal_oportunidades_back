package com.example.portal_oportunidades_back.profile.dto;

import java.time.LocalDate;

public record ExperienceResponse(Long id, String position, String organization, LocalDate startDate,
                                 LocalDate endDate, boolean current, String description) { }
