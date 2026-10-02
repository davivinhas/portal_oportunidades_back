package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.opportunity.dto.*;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.*;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpportunityService {
    private static final Sort ORDER = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
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
    public OpportunityResponse create(CreateOpportunityRequest request) {
        var details = mapper.toDetails(request);
        var recruiter = recruiters.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found"));
        return mapper.toResponse(opportunities.save(Opportunity.create(recruiter, details, clock.instant())));
    }

    public OpportunityResponse findById(Long id) { return mapper.toResponse(findActive(id)); }

    public OpportunityCursorResponse list(OpportunityFilter filter, String cursorToken, int limit) {
        if (limit < 1 || limit > 100) throw new BadRequestException("Limit must be between 1 and 100");
        var cursor = cursorCodec.decode(cursorToken, filter);
        var specification = OpportunitySpecifications.matching(filter, cursor);
        List<Opportunity> batch = opportunities.findBy(specification,
                query -> query.sortBy(ORDER).limit(limit + 1).all());
        boolean hasNext = batch.size() > limit;
        List<Opportunity> visible = hasNext ? batch.subList(0, limit) : batch;
        String nextCursor = null;
        if (hasNext) {
            Opportunity last = visible.getLast();
            nextCursor = cursorCodec.encode(new OpportunityCursor(last.getCreatedAt(), last.getId()), filter);
        }
        return new OpportunityCursorResponse(visible.stream().map(mapper::toSummary).toList(), nextCursor, hasNext);
    }

    @Transactional
    public OpportunityResponse update(Long id, UpdateOpportunityRequest request) {
        Opportunity opportunity = findActive(id);
        opportunity.updateDetails(mapper.toDetails(request), clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse publish(Long id) {
        Opportunity opportunity = findActive(id);
        opportunity.publish(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse close(Long id) {
        Opportunity opportunity = findActive(id);
        opportunity.close(clock.instant());
        return mapper.toResponse(opportunity);
    }

    @Transactional
    public void delete(Long id) { findActive(id).softDelete(clock.instant()); }

    private Opportunity findActive(Long id) {
        return opportunities.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
    }
}
