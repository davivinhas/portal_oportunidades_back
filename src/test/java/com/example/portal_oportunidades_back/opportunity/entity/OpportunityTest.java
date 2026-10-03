package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpportunityTest {
    private static final Instant START = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2030-01-31T23:59:59Z");

    @Test
    void shouldCreateOpportunityAsDraft() {
        Opportunity opportunity = opportunity(authorizedRecruiter());

        assertEquals(OpportunityStatus.DRAFT, opportunity.getStatus());
        assertFalse(opportunity.canReceiveApplications(START));
    }

    @Test
    void shouldPublishAuthorizedRecruiterOpportunity() {
        Opportunity opportunity = opportunity(authorizedRecruiter());

        opportunity.publish(START.minusSeconds(1));

        assertEquals(OpportunityStatus.PUBLISHED, opportunity.getStatus());
        assertTrue(opportunity.canReceiveApplications(START));
        assertTrue(opportunity.canReceiveApplications(END));
        assertFalse(opportunity.canReceiveApplications(START.minusSeconds(1)));
        assertFalse(opportunity.canReceiveApplications(END.plusSeconds(1)));
    }

    @Test
    void shouldRejectPublicationByUnauthorizedRecruiter() {
        Recruiter recruiter = mock(Recruiter.class);
        when(recruiter.isAuthorized()).thenReturn(false);
        Opportunity opportunity = opportunity(recruiter);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> opportunity.publish(START.minusSeconds(1)));

        assertEquals("Recruiter is not authorized to publish opportunities", exception.getMessage());
        assertEquals(OpportunityStatus.DRAFT, opportunity.getStatus());
    }

    @Test
    void shouldOnlyEditDraftOpportunity() {
        Opportunity opportunity = opportunity(authorizedRecruiter());
        opportunity.publish(START.minusSeconds(1));

        assertThrows(BusinessException.class, () -> opportunity.updateDetails(
                "New title", "New description", null, OpportunityModality.INTERNSHIP,
                null, 2, START, END));
    }

    @Test
    void shouldClosePublishedOpportunityIdempotently() {
        Opportunity opportunity = opportunity(authorizedRecruiter());
        opportunity.publish(START.minusSeconds(1));

        opportunity.close();
        opportunity.close();

        assertEquals(OpportunityStatus.CLOSED, opportunity.getStatus());
        assertFalse(opportunity.canReceiveApplications(START));
    }

    @Test
    void shouldRejectInvalidRegistrationPeriod() {
        assertThrows(BusinessException.class, () -> new Opportunity(
                authorizedRecruiter(), "Title", "Description", null,
                OpportunityModality.INTERNSHIP, null, 1, END, START));
    }

    @Test
    void shouldRejectPublicationWhenRegistrationPeriodHasEnded() {
        Opportunity opportunity = opportunity(authorizedRecruiter());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> opportunity.publish(END));

        assertEquals("Registration period has already ended", exception.getMessage());
        assertEquals(OpportunityStatus.DRAFT, opportunity.getStatus());
    }

    @Test
    void shouldPublishWhileRegistrationPeriodIsOpen() {
        Opportunity opportunity = opportunity(authorizedRecruiter());

        opportunity.publish(START);

        assertEquals(OpportunityStatus.PUBLISHED, opportunity.getStatus());
    }

    private Opportunity opportunity(Recruiter recruiter) {
        return new Opportunity(recruiter, "Backend internship", "Description", "Java",
                OpportunityModality.INTERNSHIP, "Remote", 1, START, END);
    }

    private Recruiter authorizedRecruiter() {
        Recruiter recruiter = mock(Recruiter.class);
        when(recruiter.isAuthorized()).thenReturn(true);
        return recruiter;
    }
}
