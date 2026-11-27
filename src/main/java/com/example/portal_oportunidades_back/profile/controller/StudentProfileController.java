package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.profile.dto.*;
import com.example.portal_oportunidades_back.profile.service.*;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/students/{studentId}")
@Tag(name = "Student profile", description = "Access is limited to the authenticated student")
public class StudentProfileController {
    private final StudentProfileService profiles;
    private final StudentResumeService resumes;

    public StudentProfileController(StudentProfileService profiles, StudentResumeService resumes) {
        this.profiles = profiles;
        this.resumes = resumes;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Get a student profile")
    public ResponseEntity<StudentProfileResponse> getProfile(@PathVariable Long studentId) {
        return ResponseEntity.ok(profiles.getProfile(studentId));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Create or replace a student profile",
            description = "Requires an existing user. Experiences omitted from the supplied list are deleted; an empty list clears all experiences.")
    public ResponseEntity<StudentProfileResponse> updateProfile(@PathVariable Long studentId,
            @Valid @RequestBody StudentProfileRequest request) {
        return ResponseEntity.ok(profiles.updateProfile(studentId, request));
    }

    @GetMapping(value = "/resume", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('STUDENT') and authentication.name == #studentId.toString()")
    @Operation(summary = "Download the student's current resume as PDF",
            description = "Requires name, email, registration number and course. Experience and skills are optional.")
    public ResponseEntity<byte[]> getResume(@PathVariable Long studentId) {
        byte[] pdf = resumes.generate(studentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("curriculo-" + studentId + ".pdf").build().toString())
                .cacheControl(CacheControl.noStore()).contentLength(pdf.length).body(pdf);
    }
}
