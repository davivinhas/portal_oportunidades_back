package com.example.portal_oportunidades_back.application.controller;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.application.service.ApplicationService;
import com.example.portal_oportunidades_back.opportunity.entity.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Applications", description = "Student IDs are explicit until identity and role verification are integrated")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) { this.service = service; }

    @PostMapping("/api/opportunities/{opportunityId}/applications")
    @Operation(summary = "Apply to an open opportunity",
            description = "Requires an existing student profile. Repeated applications, including after cancellation, return 409.")
    public ResponseEntity<ApplicationResponse> create(@PathVariable Long opportunityId,
            @Valid @RequestBody ApplicationCreateRequest request) {
        ApplicationResponse response = service.create(opportunityId, request.studentId());
        return ResponseEntity.created(URI.create("/api/students/" + response.studentId()
                + "/applications/" + response.id())).body(response);
    }

    @GetMapping("/api/students/{studentId}/applications")
    @Operation(summary = "List a student's applications", description = "Zero-based pagination; size 1–100. Optional status filter.")
    public ResponseEntity<ApplicationPageResponse> list(@PathVariable Long studentId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listOwned(studentId, status, page, size));
    }

    @GetMapping("/api/students/{studentId}/applications/{applicationId}")
    @Operation(summary = "Get an application owned by a student")
    public ResponseEntity<ApplicationResponse> get(@PathVariable Long studentId, @PathVariable Long applicationId) {
        return ResponseEntity.ok(service.getOwned(studentId, applicationId));
    }

    @PostMapping("/api/students/{studentId}/applications/{applicationId}/cancel")
    @Operation(summary = "Cancel an application",
            description = "Allowed while SUBMITTED or UNDER_REVIEW; repeat cancellation returns 204. Preserves the record.")
    public ResponseEntity<Void> cancel(@PathVariable Long studentId, @PathVariable Long applicationId) {
        service.cancel(studentId, applicationId);
        return ResponseEntity.noContent().build();
    }
}
