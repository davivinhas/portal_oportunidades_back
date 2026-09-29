package com.example.portal_oportunidades_back.opportunity.mapper;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OpportunityMapper {
    @Mapping(target = "recruiterId", source = "recruiter.id")
    OpportunityResponse toResponse(Opportunity opportunity);
}
