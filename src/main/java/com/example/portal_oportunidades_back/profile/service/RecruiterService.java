package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.profile.dto.RecruiterResponse;
import com.example.portal_oportunidades_back.profile.dto.RecruiterUpdateRequest;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.mapper.RecruiterMapper;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterService {
    private final RecruiterRepository recruiterRepository;
    private final RecruiterMapper recruiterMapper;

    public RecruiterService(RecruiterRepository recruiterRepository, RecruiterMapper recruiterMapper) {
        this.recruiterRepository = recruiterRepository;
        this.recruiterMapper = recruiterMapper;
    }

    @Transactional(readOnly = true)
    public RecruiterResponse getProfile(Long recruiterId) {
        return recruiterMapper.toResponse(findRecruiter(recruiterId));
    }

    @Transactional
    public RecruiterResponse updateProfile(Long recruiterId, RecruiterUpdateRequest request) {
        Recruiter recruiter = findRecruiter(recruiterId);
        recruiter.updateProfile(request.type(), request.organizationName());
        return recruiterMapper.toResponse(recruiter);
    }

    private Recruiter findRecruiter(Long recruiterId) {
        return recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
    }
}
