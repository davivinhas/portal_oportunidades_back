package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.ForbiddenOperationException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursorCodec;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
    void createsDraftOnlyForAuthorizedRecruiter() {
        Recruiter recruiter = authorizedRecruiter(7L, true);
        when(recruiters.findById(7L)).thenReturn(Optional.of(recruiter));
        when(opportunities.save(any(Opportunity.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.create(7L, createRequest());
        assertThat(result.status()).isEqualTo(OpportunityStatus.DRAFT);
        assertThat(result.recruiterId()).isEqualTo(7L);
        verify(opportunities).save(any(Opportunity.class));
    }

    @Test
    void rejectsMissingOrUnauthorizedRecruiterAtCreation() {
        when(recruiters.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(7L, createRequest())).isInstanceOf(ResourceNotFoundException.class);
        Recruiter unauthorizedRecruiter = authorizedRecruiter(8L, false);
        when(recruiters.findById(8L)).thenReturn(Optional.of(unauthorizedRecruiter));
        assertThatThrownBy(() -> service.create(8L, createRequest())).isInstanceOf(BusinessException.class);
        verifyNoInteractions(opportunities);
    }

    @Test
    void checksOwnershipBeforeRecruiterOperations() {
        Opportunity opportunity = draft(20);
        when(opportunities.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(opportunity));
        assertThatThrownBy(() -> service.getOwned(9L, 20L)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.publish(9L, 20L)).isInstanceOf(ForbiddenOperationException.class);
        verify(opportunities, times(2)).findByIdAndDeletedAtIsNull(20L);
    }

    @Test
    void updatesAndTransitionsAnOwnedOpportunityWithoutSavingAgain() {
        Opportunity opportunity = draft(20);
        when(opportunities.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(opportunity));
        assertThat(service.update(7L, 20L, updateRequest()).title()).isEqualTo("Updated title");
        assertThat(service.publish(7L, 20L).status()).isEqualTo(OpportunityStatus.PUBLISHED);
        assertThat(service.close(7L, 20L).status()).isEqualTo(OpportunityStatus.CLOSED);
        verify(opportunities, never()).save(any());
    }

    @Test
    void softDeletesAnOwnedOpportunityAndDoesNotHardDelete() {
        Opportunity opportunity = draft(20);
        when(opportunities.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(opportunity));
        service.delete(7L, 20L);
        assertThat(opportunity.getDeletedAt()).isEqualTo(NOW);
        verify(opportunities).findByIdAndDeletedAtIsNull(20L);
        verifyNoMoreInteractions(opportunities);
    }

    @Test
    void treatsMissingOrDeletedOpportunityAsNotFound() {
        when(opportunities.findByIdAndDeletedAtIsNull(30L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getOwned(7L, 30L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findPublicById(30L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publicDetailsHideDrafts() {
        Opportunity draft = draft(20);
        when(opportunities.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.findPublicById(20L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsOutOfRangePageLimitsBeforeQuerying() {
        assertThatThrownBy(() -> service.listPublic(filter(), null, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listPublic(filter(), null, 101)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(opportunities);
    }

    @Test
    void excludesNonPublishedStatusFromPublicResults() {
        var unpublishedFilter = new OpportunityFilter(null, null, OpportunityStatus.DRAFT, null);
        assertThat(service.listPublic(unpublishedFilter, null, 20).items()).isEmpty();
        verifyNoInteractions(opportunities);
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesCursorPaginationAndAnchorsCursorAtLastReturnedItem() {
        Opportunity newest = publishedOpportunity(3);
        Opportunity second = publishedOpportunity(2);
        Opportunity extra = publishedOpportunity(1);
        when(opportunities.findBy(org.mockito.ArgumentMatchers.<Specification<Opportunity>>any(),
                any(Function.class))).thenAnswer(invocation -> {
            Function<JpaSpecificationExecutor.SpecificationFluentQuery<Opportunity>, List<Opportunity>> executor =
                    invocation.getArgument(1);
            var query = mock(JpaSpecificationExecutor.SpecificationFluentQuery.class);
            when(query.sortBy(any(Sort.class))).thenReturn(query);
            when(query.limit(3)).thenReturn(query);
            when(query.all()).thenReturn(List.of(newest, second, extra));
            return executor.apply(query);
        });

        OpportunityFilter requestedFilter = new OpportunityFilter(" Research ", null, null, 7L);
        var page = service.listPublic(requestedFilter, null, 2);
        var effectiveFilter = new OpportunityFilter("research", null, OpportunityStatus.PUBLISHED, 7L);

        assertThat(page.items()).extracting(item -> item.id()).containsExactly(3L, 2L);
        assertThat(page.hasNext()).isTrue();
        assertThat(new OpportunityCursorCodec().decode(page.nextCursor(), effectiveFilter))
                .isEqualTo(new OpportunityCursor(second.getCreatedAt(), 2L));
    }

    private OpportunityFilter filter() { return new OpportunityFilter(null, null, null, null); }

    private Recruiter authorizedRecruiter(long id, boolean authorized) {
        Recruiter recruiter = mock(Recruiter.class);
        lenient().when(recruiter.getId()).thenReturn(id);
        lenient().when(recruiter.isAuthorized()).thenReturn(authorized);
        return recruiter;
    }

    private Opportunity publishedOpportunity(long id) {
        Opportunity opportunity = draft(id);
        opportunity.publish(NOW);
        return opportunity;
    }
}
