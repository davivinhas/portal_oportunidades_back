package com.example.portal_oportunidades_back.administration.controller;

import com.example.portal_oportunidades_back.administration.service.RecruiterAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;

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

    @GetMapping("/pending")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List recruiters pending authorization")
    public Page<RecruiterResponse> pending(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listPending(page, size);
    }
}
