package com.example.portal_oportunidades_back.opportunity.mapper;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunitySummaryResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OpportunityMapper {
    OpportunityDetails toDetails(OpportunityCreateRequest request);
    OpportunityDetails toDetails(OpportunityUpdateRequest request);

    @Mapping(target = "recruiterId", source = "recruiter.id")
    OpportunityResponse toResponse(Opportunity opportunity);

    @Mapping(target = "recruiterId", source = "recruiter.id")
    OpportunitySummaryResponse toSummary(Opportunity opportunity);
}
