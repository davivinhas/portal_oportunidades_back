package com.example.portal_oportunidades_back.application.dto;

import com.example.portal_oportunidades_back.profile.dto.ExperienceResponse;
import java.util.List;

public record CandidateResponse(String name, String email, String course, Short semester,
        String summary, String skills, String interests, List<ExperienceResponse> experiences) { }
