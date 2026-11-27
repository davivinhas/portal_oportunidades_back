package com.example.portal_oportunidades_back.administration.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterAuthorizationService {
    private final RecruiterRepository recruiters;

    public RecruiterAuthorizationService(RecruiterRepository recruiters) {
        this.recruiters = recruiters;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void authorize(Long recruiterId) {
        var recruiter = recruiters.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
        recruiter.authorize();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.portal_oportunidades_back.profile.dto.RecruiterResponse>
            listPending(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new com.example.portal_oportunidades_back.exception.BadRequestException("Page must be non-negative and size must be between 1 and 100");
        return recruiters.findAllByAuthorizedFalse(org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").ascending()))
                .map(recruiter -> new com.example.portal_oportunidades_back.profile.dto.RecruiterResponse(
                        recruiter.getId(), recruiter.getType(), recruiter.getOrganizationName(),
                        recruiter.isAuthorized(), recruiter.getCreatedAt(), recruiter.getUpdatedAt()));
    }
}
