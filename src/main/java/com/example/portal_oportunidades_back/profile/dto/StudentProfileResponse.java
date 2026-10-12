package com.example.portal_oportunidades_back.profile.dto;

import java.util.List;

public record StudentProfileResponse(Long id, String name, String email, String registrationNumber,
        String course, Short semester, String phone, String summary, String skills, String interests,
        List<ExperienceResponse> experiences) { }
