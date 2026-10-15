package com.example.portal_oportunidades_back.application.mapper;

import com.example.portal_oportunidades_back.application.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "studentId", source = "student.id")
    ApplicationResponse toResponse(Application application);

    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "organizationName", source = "recruiter.organizationName")
    ApplicationOpportunityResponse toResponse(Opportunity opportunity);
}
