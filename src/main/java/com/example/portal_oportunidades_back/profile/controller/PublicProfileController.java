package com.example.portal_oportunidades_back.profile.controller;

import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.dto.StudentProfileResponse;
import com.example.portal_oportunidades_back.profile.service.RecruiterService;
import com.example.portal_oportunidades_back.profile.service.StudentProfileService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicProfileController {
    private final RecruiterService recruiters;
    private final StudentProfileService students;

    public PublicProfileController(RecruiterService recruiters, StudentProfileService students) {
        this.recruiters = recruiters;
        this.students = students;
    }

    @GetMapping("/api/public/recruiters/{recruiterId}")
    @Operation(summary = "Get a public recruiter profile")
    public RecruiterResponse recruiter(@PathVariable Long recruiterId) {
        return recruiters.getPublicProfile(recruiterId);
    }

    @GetMapping("/api/public/students/{studentId}")
    @Operation(summary = "Get a candidate profile")
    public StudentProfileResponse student(@PathVariable Long studentId) {
        return students.getCandidateProfile(studentId);
    }
}
