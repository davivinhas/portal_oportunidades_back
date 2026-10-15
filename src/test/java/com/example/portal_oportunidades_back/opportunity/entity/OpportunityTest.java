package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.junit.jupiter.api.Test;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpportunityTest {
    @Test
    void createsDraftAndRequiresAuthorizedRecruiterToPublish() {
        Recruiter recruiter = mock(Recruiter.class);
        when(recruiter.isAuthorized()).thenReturn(false);
        var opportunity = new Opportunity(recruiter, details());
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.DRAFT);
        assertThatThrownBy(() -> opportunity.publish(NOW)).isInstanceOf(BusinessException.class);
        when(recruiter.isAuthorized()).thenReturn(true);
        opportunity.publish(NOW);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.PUBLISHED);
        assertThat(opportunity.isPubliclyAvailable()).isTrue();
    }

    @Test
    void validatesDetailsAndRegistrationPeriod() {
        assertThatThrownBy(() -> new OpportunityDetails(" ", "Description", null,
                OpportunityModality.INTERNSHIP, null, 1, START, END)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails("Title", "Description", null,
                OpportunityModality.INTERNSHIP, null, 0, START, END)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails("Title", "Description", null,
                OpportunityModality.INTERNSHIP, null, 1, END, START)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void onlyDraftCanBeEditedAndOnlyPublishedCanBeClosed() {
        var opportunity = draft(1);
        opportunity.updateDetails(new OpportunityDetails("Changed", "Description", null,
                OpportunityModality.SCHOLARSHIP, null, 2, START, END));
        opportunity.publish(NOW);
        assertThatThrownBy(() -> opportunity.updateDetails(details())).isInstanceOf(BusinessException.class);
        opportunity.close(NOW.plusSeconds(1));
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.CLOSED);
        assertThatThrownBy(() -> opportunity.publish(NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void softDeleteHidesOpportunityAndPreventsTransitions() {
        var opportunity = draft(1);
        opportunity.publish(NOW);
        opportunity.softDelete(NOW.plusSeconds(1));
        assertThat(opportunity.isPubliclyAvailable()).isFalse();
        assertThatThrownBy(() -> opportunity.close(NOW.plusSeconds(2))).isInstanceOf(BusinessException.class);
    }
}
