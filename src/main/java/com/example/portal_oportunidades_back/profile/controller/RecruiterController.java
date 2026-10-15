package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.dto.RecruiterUpdateRequest;
import com.example.portal_oportunidades_back.profile.service.RecruiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruiters/{recruiterId}/profile")
@Tag(name = "Recruiter profile")
public class RecruiterController {
    private final RecruiterService recruiterService;

    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }

    @GetMapping
    @Operation(summary = "Get a recruiter profile")
    public ResponseEntity<RecruiterResponse> getProfile(@PathVariable Long recruiterId) {
        return ResponseEntity.ok(recruiterService.getProfile(recruiterId));
    }

    @PutMapping
    @Operation(summary = "Update a recruiter profile")
    public ResponseEntity<RecruiterResponse> updateProfile(
            @PathVariable Long recruiterId, @Valid @RequestBody RecruiterUpdateRequest request) {
        return ResponseEntity.ok(recruiterService.updateProfile(recruiterId, request));
    }
}
