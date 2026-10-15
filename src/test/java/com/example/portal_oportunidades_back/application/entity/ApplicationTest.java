package com.example.portal_oportunidades_back.application.entity;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.opportunity.entity.*;
import com.example.portal_oportunidades_back.profile.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationTest {
    private final Student student = mock(Student.class);

    private Application submitted() {
        Opportunity opportunity = draft(1L);
        opportunity.publish(NOW);
        return new Application(student, opportunity, NOW);
    }

    @Test
    void startsSubmittedAndAcceptsBothRegistrationBoundaries() {
        Opportunity opportunity = draft(1L);
        opportunity.publish(NOW);
        assertThat(new Application(student, opportunity, START).getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(new Application(student, opportunity, END).getAppliedAt()).isEqualTo(END);
    }

    @Test
    void rejectsOutsidePeriodAndUnavailableOpportunities() {
        Opportunity opportunity = draft(1L);
        assertThatThrownBy(() -> new Application(student, opportunity, NOW)).isInstanceOf(BusinessException.class);
        opportunity.publish(NOW);
        assertThatThrownBy(() -> new Application(student, opportunity, START.minusSeconds(1))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new Application(student, opportunity, END.plusSeconds(1))).isInstanceOf(BusinessException.class);
        opportunity.close(NOW);
        assertThatThrownBy(() -> new Application(student, opportunity, NOW)).isInstanceOf(BusinessException.class);
        Opportunity deleted = draft(2L);
        deleted.publish(NOW);
        deleted.softDelete(NOW);
        assertThatThrownBy(() -> new Application(student, deleted, NOW)).isInstanceOf(BusinessException.class);
    }

    @Test
    void transitionsForwardAndRejectsRegressionAndNull() {
        Application application = submitted();
        application.updateStatus(ApplicationStatus.UNDER_REVIEW);
        assertThatThrownBy(() -> application.updateStatus(ApplicationStatus.SUBMITTED)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> application.updateStatus(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> application.updateStatus(ApplicationStatus.CANCELLED)).isInstanceOf(BusinessException.class);
        application.updateStatus(ApplicationStatus.APPROVED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"APPROVED", "REJECTED"})
    void protectsFinalStatuses(ApplicationStatus terminal) {
        Application application = submitted();
        application.updateStatus(terminal);
        application.updateStatus(terminal);
        assertThatThrownBy(application::cancel).isInstanceOf(BusinessException.class);
        for (ApplicationStatus target : ApplicationStatus.values()) {
            if (target != terminal)
                assertThatThrownBy(() -> application.updateStatus(target)).isInstanceOf(BusinessException.class);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"SUBMITTED", "UNDER_REVIEW"})
    void cancelsOnlyActiveStatesAndPreservesTerminalCancellation(ApplicationStatus initial) {
        Application application = submitted();
        application.updateStatus(initial);
        application.cancel();
        application.cancel();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThatThrownBy(() -> application.updateStatus(ApplicationStatus.APPROVED)).isInstanceOf(BusinessException.class);
    }
}
