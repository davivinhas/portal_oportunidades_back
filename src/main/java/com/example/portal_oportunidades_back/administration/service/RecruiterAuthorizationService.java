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
}
