package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.ForbiddenOperationException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCursorResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunitySummaryResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursorCodec;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.query.OpportunitySpecifications;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpportunityService {
    private static final Sort PUBLIC_ORDER = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OpportunityRepository opportunities;
    private final RecruiterRepository recruiters;
    private final OpportunityMapper mapper;
    private final OpportunityCursorCodec cursorCodec;
    private final Clock clock;

    public OpportunityService(OpportunityRepository opportunities, RecruiterRepository recruiters,
            OpportunityMapper mapper, OpportunityCursorCodec cursorCodec, Clock clock) {
        this.opportunities = opportunities;
        this.recruiters = recruiters;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.clock = clock;
    }

    @Transactional
    public OpportunityResponse create(Long recruiterId, OpportunityCreateRequest request) {
        Recruiter recruiter = findRecruiter(recruiterId);
        if (!recruiter.isAuthorized()) {
            throw new BusinessException("Recruiter is not authorized to create opportunities");
        }
        Opportunity opportunity = new Opportunity(recruiter, mapper.toDetails(request));
        return mapper.toResponse(opportunities.save(opportunity));
    }

    public List<OpportunityResponse> listOwned(Long recruiterId) {
        ensureRecruiterExists(recruiterId);
        return opportunities.findAllByRecruiterIdAndDeletedAtIsNullOrderByCreatedAtDesc(recruiterId)
                .stream().map(mapper::toResponse).toList();
    }

    public OpportunityResponse getOwned(Long recruiterId, Long opportunityId) {
        return mapper.toResponse(findOwned(recruiterId, opportunityId));
    }

    @Transactional
    public OpportunityResponse update(Long recruiterId, Long opportunityId, OpportunityUpdateRequest request) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        ensureAuthorized(opportunity.getRecruiter());
        opportunity.updateDetails(mapper.toDetails(request));
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse publish(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        ensureAuthorized(opportunity.getRecruiter());
        opportunity.publish(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse close(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        ensureAuthorized(opportunity.getRecruiter());
        opportunity.close(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public void delete(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = findOwned(recruiterId, opportunityId);
        ensureAuthorized(opportunity.getRecruiter());
        opportunity.softDelete(clock.instant());
    }

    public OpportunityCursorResponse listPublic(OpportunityFilter filter, String cursorToken, int limit) {
        if (limit < 1 || limit > 100) throw new BadRequestException("Limit must be between 1 and 100");
        if (filter.status() != null && filter.status() != OpportunityStatus.PUBLISHED) {
            return new OpportunityCursorResponse(List.of(), null, false);
        }
        OpportunityFilter publicFilter = new OpportunityFilter(filter.title(), filter.modality(),
                OpportunityStatus.PUBLISHED, filter.recruiterId());
        OpportunityCursor cursor = cursorCodec.decode(cursorToken, publicFilter);
        var specification = OpportunitySpecifications.matching(publicFilter, cursor);
        List<Opportunity> batch = opportunities.findBy(specification,
                query -> query.sortBy(PUBLIC_ORDER).limit(limit + 1).all());
        boolean hasNext = batch.size() > limit;
        List<Opportunity> visible = hasNext ? batch.subList(0, limit) : batch;
        String nextCursor = null;
        if (hasNext) {
            Opportunity last = visible.getLast();
            nextCursor = cursorCodec.encode(new OpportunityCursor(last.getCreatedAt(), last.getId()), publicFilter);
        }
        List<OpportunitySummaryResponse> items = visible.stream().map(mapper::toSummary).toList();
        return new OpportunityCursorResponse(items, nextCursor, hasNext);
    }

    public OpportunityResponse findPublicById(Long id) {
        Opportunity opportunity = opportunities.findByIdAndDeletedAtIsNull(id)
                .filter(Opportunity::isPubliclyAvailable)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
        return mapper.toResponse(opportunity);
    }

    private Opportunity findOwned(Long recruiterId, Long opportunityId) {
        Opportunity opportunity = opportunities.findByIdAndDeletedAtIsNull(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        if (!opportunity.belongsTo(recruiterId)) {
            throw new ForbiddenOperationException("Opportunity does not belong to recruiter: " + recruiterId);
        }
        return opportunity;
    }

    private Recruiter findRecruiter(Long recruiterId) {
        return recruiters.findById(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + recruiterId));
    }

    private void ensureAuthorized(Recruiter recruiter) {
        if (!recruiter.isAuthorized()) {
            throw new ForbiddenOperationException("Recruiter is not authorized to manage opportunities");
        }
    }

    private void ensureRecruiterExists(Long recruiterId) {
        if (!recruiters.existsById(recruiterId)) {
            throw new ResourceNotFoundException("Recruiter not found: " + recruiterId);
        }
    }
}
