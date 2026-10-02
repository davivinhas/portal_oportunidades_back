package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursorCodec;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import static com.example.portal_oportunidades_back.opportunity.OpportunityFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {
    @Mock OpportunityRepository opportunities;
    @Mock RecruiterRepository recruiters;
    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunities, recruiters,
                Mappers.getMapper(OpportunityMapper.class), new OpportunityCursorCodec(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsDraftForExistingRecruiter() {
        Recruiter recruiter = mock(Recruiter.class);
        when(recruiters.findById(7L)).thenReturn(Optional.of(recruiter));
        when(opportunities.save(any(Opportunity.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(createRequest());

        assertThat(response.status()).isEqualTo(OpportunityStatus.RASCUNHO);
        assertThat(response.title()).isEqualTo(createRequest().title());
        verify(opportunities).save(argThat(opportunity -> opportunity.getRecruiter() == recruiter
                && opportunity.getStatus() == OpportunityStatus.RASCUNHO));
    }

    @Test
    void rejectsMissingRecruiterWithoutSaving() {
        when(recruiters.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recruiter not found");

        verify(opportunities, never()).save(any());
    }

    @Test
    void readsExistingOpportunityAndReturnsNotFoundForMissingOrDeleted() {
        Opportunity opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));
        assertThat(service.findById(10L).id()).isEqualTo(10L);

        when(opportunities.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(11L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesManagedEntityWithoutCallingSave() {
        Opportunity opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));

        var response = service.update(10L, updateRequest());

        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.requirements()).isNull();
        verify(opportunities, never()).save(any());
    }

    @Test
    void publishesAndClosesThroughDomainTransitions() {
        Opportunity opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));

        assertThat(service.publish(10L).status()).isEqualTo(OpportunityStatus.PUBLICADA);
        assertThat(service.close(10L).status()).isEqualTo(OpportunityStatus.ENCERRADA);

        verify(opportunities, never()).save(any());
    }

    @Test
    void softDeletesLoadedOpportunityWithoutCallingRepositoryDelete() {
        Opportunity opportunity = draft(10);
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(opportunity));

        service.delete(10L);

        assertThat(opportunity.getDeletedAt()).isEqualTo(NOW);
        verify(opportunities).findByIdAndDeletedAtIsNull(10L);
        verifyNoMoreInteractions(opportunities);
    }

    @Test
    void returnsNotFoundForEveryMutationOfMissingOrDeletedOpportunity() {
        when(opportunities.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, updateRequest())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.publish(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.close(10L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(ResourceNotFoundException.class);
        verify(opportunities, times(4)).findByIdAndDeletedAtIsNull(10L);
        verifyNoMoreInteractions(opportunities);
    }

    @Test
    void rejectsPageLimitsOutsideSupportedRangeBeforeQuerying() {
        assertThatThrownBy(() -> service.list(null, null, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(null, null, 101)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(opportunities, recruiters);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsOnlyRequestedItemsAndBuildsCursorFromLastReturnedItem() {
        Opportunity newest = draft(3);
        Opportunity second = draft(2);
        Opportunity extra = draft(1);
        when(opportunities.findBy(org.mockito.ArgumentMatchers.<Specification<Opportunity>>any(),
                any(Function.class))).thenAnswer(invocation -> {
            Function<JpaSpecificationExecutor.SpecificationFluentQuery<Opportunity>, List<Opportunity>> queryFunction =
                    invocation.getArgument(1);
            var fluentQuery = mock(JpaSpecificationExecutor.SpecificationFluentQuery.class);
            when(fluentQuery.sortBy(any(Sort.class))).thenReturn(fluentQuery);
            when(fluentQuery.limit(3)).thenReturn(fluentQuery);
            when(fluentQuery.all()).thenReturn(List.of(newest, second, extra));
            return queryFunction.apply(fluentQuery);
        });

        var filter = new com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter(
                null, null, null, null);
        var page = service.list(filter, null, 2);

        assertThat(page.items()).extracting(item -> item.id()).containsExactly(3L, 2L);
        assertThat(page.hasNext()).isTrue();
        assertThat(new OpportunityCursorCodec().decode(page.nextCursor(), filter))
                .isEqualTo(new com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor(
                        second.getCreatedAt(), 2L));
        verify(opportunities).findBy(org.mockito.ArgumentMatchers.<Specification<Opportunity>>any(),
                any(Function.class));
    }
}
