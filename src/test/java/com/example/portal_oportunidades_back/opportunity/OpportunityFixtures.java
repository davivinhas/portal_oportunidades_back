package com.example.portal_oportunidades_back.opportunity;

import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityDetails;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public final class OpportunityFixtures {
    public static final Instant NOW = Instant.parse("2030-01-10T12:00:00Z");
    public static final Instant START = NOW.minusSeconds(60);
    public static final Instant END = NOW.plusSeconds(3600);

    private OpportunityFixtures() { }

    public static OpportunityDetails details() {
        return new OpportunityDetails("Research assistant", "Research activities", "Java",
                OpportunityModality.SCIENTIFIC_INITIATION, "São Luís", 2, START, END);
    }

    public static Opportunity draft(long id) {
        Recruiter recruiter = mock(Recruiter.class);
        lenient().when(recruiter.getId()).thenReturn(7L);
        lenient().when(recruiter.isAuthorized()).thenReturn(true);
        Opportunity opportunity = new Opportunity(recruiter, details());
        ReflectionTestUtils.setField(opportunity, "id", id);
        ReflectionTestUtils.setField(opportunity, "createdAt", NOW);
        ReflectionTestUtils.setField(opportunity, "updatedAt", NOW);
        return opportunity;
    }

    public static OpportunityCreateRequest createRequest() {
        return new OpportunityCreateRequest("Research assistant", "Research activities", "Java",
                OpportunityModality.SCIENTIFIC_INITIATION, "São Luís", 2, START, END);
    }

    public static OpportunityUpdateRequest updateRequest() {
        return new OpportunityUpdateRequest("Updated title", "Updated description", null,
                OpportunityModality.SCIENTIFIC_INITIATION, null, 3, START, END);
    }
}
