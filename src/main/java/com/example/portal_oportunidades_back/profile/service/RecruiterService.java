package com.example.portal_oportunidades_back.profile.service;

import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.administration.repository.CatalogItemRepository;
import com.example.portal_oportunidades_back.administration.service.CatalogAdministrationService;
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
    private final CatalogItemRepository catalogs;

    @org.springframework.beans.factory.annotation.Autowired
    public RecruiterService(RecruiterRepository recruiterRepository, RecruiterMapper recruiterMapper,
            CatalogItemRepository catalogs) {
        this.recruiterRepository = recruiterRepository;
        this.recruiterMapper = recruiterMapper;
        this.catalogs = catalogs;
    }

    public RecruiterService(RecruiterRepository recruiterRepository, RecruiterMapper recruiterMapper) {
        this(recruiterRepository, recruiterMapper, null);
    }

    @Transactional(readOnly = true)
    public RecruiterResponse getProfile(Long recruiterId) {
        return recruiterMapper.toResponse(findRecruiter(recruiterId));
    }

    @Transactional(readOnly = true)
    public RecruiterResponse getPublicProfile(Long recruiterId) {
        return recruiterMapper.toResponse(findRecruiter(recruiterId));
    }

    @Transactional
    public RecruiterResponse updateProfile(Long recruiterId, RecruiterUpdateRequest request) {
        validateType(request.type());
        Recruiter recruiter = findRecruiter(recruiterId);
        recruiter.updateProfile(request.type(), request.organizationName(), request.description(), request.focusArea(), request.website());
        return recruiterMapper.toResponse(recruiter);
    }

    private Recruiter findRecruiter(Long recruiterId) {
        return recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
    }

    private void validateType(String type) {
        if (catalogs != null && !catalogs.existsByCategoryAndCodeAndActiveTrue(CatalogAdministrationService.RECRUITER_TYPE, type))
            throw new BadRequestException("Recruiter type is not active in the catalog");
    }
}
