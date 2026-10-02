package com.example.portal_oportunidades_back.opportunity.mapper;

import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OpportunityMapper {
    OpportunityDetails toDetails(CreateOpportunityRequest request);
    OpportunityDetails toDetails(UpdateOpportunityRequest request);
    OpportunityResponse toResponse(Opportunity opportunity);
    OpportunitySummaryResponse toSummary(Opportunity opportunity);
    RecruiterSummaryResponse toRecruiterSummary(Recruiter recruiter);
}