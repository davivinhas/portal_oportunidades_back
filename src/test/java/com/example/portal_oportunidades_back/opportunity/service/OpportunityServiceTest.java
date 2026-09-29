package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.ForbiddenOperationException;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityModality;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {
    @Mock OpportunityRepository opportunityRepository;
    @Mock RecruiterRepository recruiterRepository;
    @Mock OpportunityMapper opportunityMapper;
    @Mock Recruiter recruiter;
    @Mock Opportunity opportunity;
    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunityRepository, recruiterRepository, opportunityMapper);
    }

    @Test
    void shouldCreateDraftForAuthorizedRecruiter() {
        OpportunityCreateRequest request = request();
        OpportunityResponse response = mock(OpportunityResponse.class);
        when(recruiterRepository.findById(10L)).thenReturn(Optional.of(recruiter));
        when(recruiter.isAuthorized()).thenReturn(true);
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(opportunityMapper.toResponse(any(Opportunity.class))).thenReturn(response);

        assertSame(response, service.create(10L, request));
        verify(opportunityRepository).save(any(Opportunity.class));
    }

    @Test
    void shouldRejectCreationForUnauthorizedRecruiter() {
        when(recruiterRepository.findById(10L)).thenReturn(Optional.of(recruiter));
        when(recruiter.isAuthorized()).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.create(10L, request()));
        verifyNoInteractions(opportunityRepository);
    }

    @Test
    void shouldRejectAccessToAnotherRecruiterOpportunity() {
        when(opportunityRepository.findById(20L)).thenReturn(Optional.of(opportunity));
        when(opportunity.belongsTo(10L)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> service.getOwned(10L, 20L));
    }

    @Test
    void shouldPublishOwnedOpportunity() {
        OpportunityResponse response = mock(OpportunityResponse.class);
        when(opportunityRepository.findById(20L)).thenReturn(Optional.of(opportunity));
        when(opportunity.belongsTo(10L)).thenReturn(true);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(response);

        assertSame(response, service.publish(10L, 20L));
        verify(opportunity).publish();
    }

    @Test
    void shouldCloseOwnedOpportunity() {
        OpportunityResponse response = mock(OpportunityResponse.class);
        when(opportunityRepository.findById(20L)).thenReturn(Optional.of(opportunity));
        when(opportunity.belongsTo(10L)).thenReturn(true);
        when(opportunityMapper.toResponse(opportunity)).thenReturn(response);

        assertSame(response, service.close(10L, 20L));
        verify(opportunity).close();
    }

    private OpportunityCreateRequest request() {
        return new OpportunityCreateRequest("Title", "Description", "Java",
                OpportunityModality.INTERNSHIP, "Remote", 2,
                Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-02-01T00:00:00Z"));
    }
}
