package com.example.portal_oportunidades_back.opportunity.entity;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import org.junit.jupiter.api.Test;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OpportunityTest {
    @Test
    void createsCompleteDraft() {
        var opportunity = Opportunity.create(mock(Recruiter.class), details(), NOW);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.RASCUNHO);
        assertThat(opportunity.getCreatedAt()).isEqualTo(NOW);
        assertThat(opportunity.getUpdatedAt()).isEqualTo(NOW);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
    }

    @Test
    void rejectsInvalidPeriodAndVacancyCount() {
        assertThatThrownBy(() -> new OpportunityDetails("Title", "Description", null,
                OpportunityModality.IC, null, 1, END, START)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails("Title", "Description", null,
                OpportunityModality.IC, null, 1, START, START)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new OpportunityDetails("Title", "Description", null,
                OpportunityModality.IC, null, 0, START, END)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void publishesAndHonorsRegistrationBoundaries() {
        var opportunity = draft(1);
        opportunity.publish(NOW);
        assertThat(opportunity.canReceiveApplications(START.minusNanos(1))).isFalse();
        assertThat(opportunity.canReceiveApplications(START)).isTrue();
        assertThat(opportunity.canReceiveApplications(END.minusNanos(1))).isTrue();
        assertThat(opportunity.canReceiveApplications(END)).isFalse();
        assertThatThrownBy(() -> opportunity.publish(NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsPublishingBeforeRegistrationStarts() {
        var details = new OpportunityDetails("Title", "Description", null,
                OpportunityModality.IC, null, 1, NOW.plusSeconds(60), END);
        var opportunity = Opportunity.create(mock(Recruiter.class), details, NOW);
        opportunity.publish(NOW);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
    }

    @Test
    void rejectsPublishingAtDeadlineWithoutChangingState() {
        var opportunity = draft(1);
        assertThatThrownBy(() -> opportunity.publish(END)).isInstanceOf(BusinessException.class);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.RASCUNHO);
    }

    @Test
    void updatesDraftAndPublishedButRejectsExpiredPeriodAtomically() {
        var opportunity = draft(1);
        var replacement = new OpportunityDetails("New title", "New description", null,
                OpportunityModality.BOLSA, null, 4, START, END);
        opportunity.updateDetails(replacement, NOW);
        assertThat(opportunity.getTitle()).isEqualTo("New title");
        assertThat(opportunity.getRequirements()).isNull();
        opportunity.publish(NOW);
        opportunity.updateDetails(details(), NOW.plusSeconds(1));

        var expired = new OpportunityDetails("Should not apply", "Description", null,
                OpportunityModality.IC, null, 1, START, NOW);
        assertThatThrownBy(() -> opportunity.updateDetails(expired, NOW)).isInstanceOf(BusinessException.class);
        assertThat(opportunity.getTitle()).isEqualTo(details().title());
    }

    @Test
    void closesOnlyPublishedAndNeverReopens() {
        var opportunity = draft(1);
        assertThatThrownBy(() -> opportunity.close(NOW)).isInstanceOf(BusinessException.class);
        opportunity.publish(NOW);
        opportunity.close(NOW.plusSeconds(1));
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.ENCERRADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
        assertThatThrownBy(() -> opportunity.updateDetails(details(), NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.publish(NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.close(NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void softDeletePreservesStatusAndBlocksFurtherChanges() {
        var opportunity = draft(1);
        opportunity.publish(NOW);
        opportunity.softDelete(NOW.plusSeconds(2));
        assertThat(opportunity.getDeletedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(opportunity.canReceiveApplications(NOW)).isFalse();
        assertThatThrownBy(() -> opportunity.updateDetails(details(), NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.publish(NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.close(NOW)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> opportunity.softDelete(NOW)).isInstanceOf(BusinessException.class);
    }
}
