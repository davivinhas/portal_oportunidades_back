package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.profile.dto.FollowingResponse;
import com.example.portal_oportunidades_back.profile.service.RecruiterFollowService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/students/{studentId}/following")
@Tag(name = "Recruiter following", description = "Access is limited to the authenticated student")
public class RecruiterFollowController {
    private final RecruiterFollowService service;

    public RecruiterFollowController(RecruiterFollowService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "List recruiters followed by a student")
    public ResponseEntity<java.util.List<com.example.portal_oportunidades_back.profile.dto.RecruiterResponse>> list(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(service.listFollowing(studentId));
    }

    @PutMapping("/{recruiterId}")
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Follow a recruiter", description = "Idempotent.")
    public ResponseEntity<Void> follow(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        service.follow(studentId, recruiterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{recruiterId}")
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Unfollow a recruiter", description = "Idempotent, including when the follow does not exist.")
    public ResponseEntity<Void> unfollow(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        service.unfollow(studentId, recruiterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{recruiterId}")
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Check whether a student follows a recruiter")
    public ResponseEntity<FollowingResponse> getFollowing(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        return ResponseEntity.ok(service.getFollowing(studentId, recruiterId));
    }
}
