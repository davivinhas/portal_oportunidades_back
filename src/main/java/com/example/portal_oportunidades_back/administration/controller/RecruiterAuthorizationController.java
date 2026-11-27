package com.example.portal_oportunidades_back.administration.controller;

import com.example.portal_oportunidades_back.administration.service.RecruiterAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/recruiters")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class RecruiterAuthorizationController {
    private final RecruiterAuthorizationService service;

    public RecruiterAuthorizationController(RecruiterAuthorizationService service) {
        this.service = service;
    }

    @PutMapping("/{recruiterId}/authorization")
    @Operation(summary = "Authorize a recruiter", description = "Administrator only. Repeated authorization is safe.")
    public ResponseEntity<Void> authorize(@PathVariable Long recruiterId) {
        service.authorize(recruiterId);
        return ResponseEntity.noContent().build();
    }
}
