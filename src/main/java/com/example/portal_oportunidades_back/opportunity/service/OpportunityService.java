package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.ForbiddenOperationException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OpportunityService {
    private final OpportunityRepository opportunityRepository;
    private final RecruiterRepository recruiterRepository;
    private final OpportunityMapper opportunityMapper;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              RecruiterRepository recruiterRepository,
                              OpportunityMapper opportunityMapper) {
        this.opportunityRepository = opportunityRepository;
        this.recruiterRepository = recruiterRepository;
        this.opportunityMapper = opportunityMapper;
    }

    @Transactional
    public OpportunityResponse create(Long recruiterId, OpportunityCreateRequest request) {
        Recruiter recruiter = findRecruiter(recruiterId);
        if (!recruiter.isAuthorized()) {
            throw new BusinessException("Recruiter is not authorized to create opportunities");
        }
        Opportunity opportunity = new Opportunity(recruiter, request.title(), request.description(),
                request.requirements(), request.modality(), request.location(), request.vacancyCount(),
                request.registrationStartsAt(), request.registrationEndsAt());
        return opportunityMapper.toResponse(opportunityRepository.save(opportunity));
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> listOwned(Long recruiterId) {
        ensureRecruiterExists(recruiterId);
        return opportunityRepository.findAllByRecruiterIdOrderByCreatedAtDesc(recruiterId).stream()
                .map(opportunityMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getOwned(Long recruiterId, Long opportunityId) {
        return opportunityMapper.toResponse(findOwned(recruiterId, opportunityId));
    }

    @Transactional
    public OpportunityResponse update(Long recruiterId, Long opportunityId, OpportunityUpdateRequest request) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        opportunity.updateDetails(request.title(), request.description(), request.requirements(),
                request.modality(), request.location(), request.vacancyCount(),
                request.registrationStartsAt(), request.registrationEndsAt());
        return opportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse publish(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        opportunity.publish();
        return opportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse close(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        opportunity.close();
        return opportunityMapper.toResponse(opportunity);
    }

    private Opportunity findOwned(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        if (!opportunity.belongsTo(recruiterId)) {
            throw new ForbiddenOperationException("Opportunity does not belong to recruiter: " + recruiterId);
        }
        return opportunity;
    }

    private Recruiter findRecruiter(Long recruiterId) {
        return recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
    }

    private void ensureRecruiterExists(Long recruiterId) {
        if (!recruiterRepository.existsById(recruiterId)) {
            throw new ResourceNotFoundException("Recruiter not found: " + recruiterId);
        }
    }
}
