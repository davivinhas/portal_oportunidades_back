package com.example.portal_oportunidades_back.profile.mapper;

import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecruiterMapper {
    @Mapping(target = "authorized", source = "authorized")
    RecruiterResponse toResponse(Recruiter recruiter);
}
