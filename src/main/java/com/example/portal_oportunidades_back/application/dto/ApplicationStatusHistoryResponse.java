package com.example.portal_oportunidades_back.application.dto;
import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import java.time.Instant;
public record ApplicationStatusHistoryResponse(Long id, ApplicationStatus previousStatus, ApplicationStatus newStatus, Instant changedAt) { }
