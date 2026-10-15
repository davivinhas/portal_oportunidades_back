package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.profile.dto.FollowingResponse;
import com.example.portal_oportunidades_back.profile.service.RecruiterFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/{studentId}/following/{recruiterId}")
@Tag(name = "Recruiter following", description = "Student identity and ownership verification are pending authentication integration")
public class RecruiterFollowController {
    private final RecruiterFollowService service;

    public RecruiterFollowController(RecruiterFollowService service) {
        this.service = service;
    }

    @PutMapping
    @Operation(summary = "Follow a recruiter", description = "Idempotent. Student ID is supplied explicitly until authentication integration.")
    public ResponseEntity<Void> follow(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        service.follow(studentId, recruiterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Unfollow a recruiter", description = "Idempotent, including when the follow does not exist.")
    public ResponseEntity<Void> unfollow(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        service.unfollow(studentId, recruiterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Check whether a student follows a recruiter")
    public ResponseEntity<FollowingResponse> getFollowing(@PathVariable Long studentId, @PathVariable Long recruiterId) {
        return ResponseEntity.ok(service.getFollowing(studentId, recruiterId));
    }
}
