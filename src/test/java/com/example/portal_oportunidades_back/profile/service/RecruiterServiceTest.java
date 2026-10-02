package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.dto.RecruiterUpdateRequest;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.entity.RecruiterType;
import com.example.portal_oportunidades_back.profile.mapper.RecruiterMapper;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceTest {
    @Mock RecruiterRepository recruiterRepository;
    @Mock RecruiterMapper recruiterMapper;
    @Mock Recruiter recruiter;
    private RecruiterService service;

    @BeforeEach
    void setUp() {
        service = new RecruiterService(recruiterRepository, recruiterMapper);
    }

    @Test
    void shouldUpdateRecruiterProfile() {
        RecruiterUpdateRequest request = new RecruiterUpdateRequest(RecruiterType.PROFESSOR, "Researcher");
        RecruiterResponse response = mock(RecruiterResponse.class);
        when(recruiterRepository.findById(10L)).thenReturn(Optional.of(recruiter));
        when(recruiterMapper.toResponse(recruiter)).thenReturn(response);

        RecruiterResponse result = service.updateProfile(10L, request);

        assertSame(response, result);
        verify(recruiter).updateProfile(RecruiterType.PROFESSOR, "Researcher");
        verify(recruiterRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenRecruiterDoesNotExist() {
        when(recruiterRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getProfile(10L));
    }
}
