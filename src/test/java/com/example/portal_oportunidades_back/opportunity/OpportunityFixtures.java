package com.example.portal_oportunidades_back.opportunity;

import com.example.portal_oportunidades_back.opportunity.dto.CreateOpportunityRequest;
import com.example.portal_oportunidades_back.opportunity.dto.UpdateOpportunityRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityDetails;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.mock;

public final class OpportunityFixtures {
    public static final Instant NOW = Instant.parse("2030-01-10T12:00:00Z");
    public static final Instant START = NOW.minusSeconds(60);
    public static final Instant END = NOW.plusSeconds(3600);

    private OpportunityFixtures() { }

    public static OpportunityDetails details() {
        return new OpportunityDetails("Research assistant", "Research activities", "Java",
                OpportunityModality.IC, "São Luís", 2, START, END);
    }

    public static Opportunity draft(long id) {
        Opportunity opportunity = Opportunity.create(mock(Recruiter.class), details(), NOW);
        ReflectionTestUtils.setField(opportunity, "id", id);
        return opportunity;
    }

    public static CreateOpportunityRequest createRequest() {
        return new CreateOpportunityRequest(7L, "Research assistant", "Research activities",
                "Java", OpportunityModality.IC, "São Luís", 2, START, END);
    }

    public static UpdateOpportunityRequest updateRequest() {
        return new UpdateOpportunityRequest("Updated title", "Updated description",
                null, OpportunityModality.IC, null, 3, START, END);
    }
}
