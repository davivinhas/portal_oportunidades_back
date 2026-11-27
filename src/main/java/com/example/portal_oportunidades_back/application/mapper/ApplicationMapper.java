package com.example.portal_oportunidades_back.application.mapper;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "candidate", source = "student")
    ApplicationResponse toResponse(Application application);

    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "course", source = "course")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "summary", source = "summary")
    @Mapping(target = "skills", source = "skills")
    @Mapping(target = "interests", source = "interests")
    @Mapping(target = "experiences", ignore = true)
    com.example.portal_oportunidades_back.application.dto.CandidateResponse toCandidate(
            com.example.portal_oportunidades_back.profile.entity.Student student);

    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "organizationName", source = "recruiter.organizationName")
    ApplicationOpportunityResponse toResponse(Opportunity opportunity);
}
