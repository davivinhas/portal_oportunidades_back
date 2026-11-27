package com.example.portal_oportunidades_back.opportunity.service;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.exception.BusinessException;
import com.example.portal_oportunidades_back.exception.ForbiddenOperationException;
import com.example.portal_oportunidades_back.exception.ResourceNotFoundException;
import com.example.portal_oportunidades_back.administration.repository.CatalogItemRepository;
import com.example.portal_oportunidades_back.administration.service.CatalogAdministrationService;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCreateRequest;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityCursorResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunitySummaryResponse;
import com.example.portal_oportunidades_back.opportunity.dto.OpportunityUpdateRequest;
import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.mapper.OpportunityMapper;
import com.example.portal_oportunidades_back.opportunity.query.BooleanSearchQueryParser;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursorCodec;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import com.example.portal_oportunidades_back.opportunity.query.OpportunitySpecifications;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunityRepository;
import com.example.portal_oportunidades_back.opportunity.repository.OpportunitySearchHit;
import com.example.portal_oportunidades_back.profile.entity.Recruiter;
import com.example.portal_oportunidades_back.profile.repository.RecruiterRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpportunityService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Sort PUBLIC_ORDER = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OpportunityRepository opportunities;
    private final RecruiterRepository recruiters;
    private final OpportunityMapper mapper;
    private final OpportunityCursorCodec cursorCodec;
    private final BooleanSearchQueryParser searchQueryParser;
    private final Clock clock;
    private final CatalogItemRepository catalogs;

    @Autowired
    public OpportunityService(OpportunityRepository opportunities, RecruiterRepository recruiters,
            OpportunityMapper mapper, OpportunityCursorCodec cursorCodec,
            BooleanSearchQueryParser searchQueryParser, Clock clock, CatalogItemRepository catalogs) {
        this.opportunities = opportunities;
        this.recruiters = recruiters;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.searchQueryParser = searchQueryParser;
        this.clock = clock;
        this.catalogs = catalogs;
    }

    public OpportunityService(OpportunityRepository opportunities, RecruiterRepository recruiters,
            OpportunityMapper mapper, OpportunityCursorCodec cursorCodec,
            BooleanSearchQueryParser searchQueryParser, Clock clock) {
        this(opportunities, recruiters, mapper, cursorCodec, searchQueryParser, clock, null);
    }

    public OpportunityService(OpportunityRepository opportunities, RecruiterRepository recruiters,
            OpportunityMapper mapper, OpportunityCursorCodec cursorCodec, Clock clock) {
        this(opportunities, recruiters, mapper, cursorCodec, new BooleanSearchQueryParser(), clock);
    }

    @Transactional
    public OpportunityResponse create(Long recruiterId, OpportunityCreateRequest request) {
        validateRegistrationStart(request.registrationStartsAt());
        validateModality(request.modality());
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
        validateRegistrationStart(request.registrationStartsAt());
        validateModality(request.modality());
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
        OpportunityFilter publicFilter = new OpportunityFilter(filter.query(), filter.modality(),
                OpportunityStatus.PUBLISHED, filter.recruiterId(), filter.location(), filter.area(),
                filter.deadlineFrom(), filter.deadlineTo());
        OpportunityCursor cursor = cursorCodec.decode(cursorToken, publicFilter);
        if (publicFilter.query() != null) {
            return listPublicByQuery(publicFilter, cursor, limit);
        }
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

    private OpportunityCursorResponse listPublicByQuery(OpportunityFilter filter,
            OpportunityCursor cursor, int limit) {
        BooleanSearchQueryParser.SearchNode parsedQuery = searchQueryParser.parse(filter.query());
        List<OpportunitySearchHit> hits = opportunities.searchPublic(filter, parsedQuery, cursor, limit);
        boolean hasNext = hits.size() > limit;
        List<OpportunitySearchHit> pageHits = hasNext ? hits.subList(0, limit) : hits;

        Map<Long, Opportunity> loadedById = new HashMap<>();
        if (!pageHits.isEmpty()) {
            opportunities.findAllByIdInAndDeletedAtIsNull(
                    pageHits.stream().map(OpportunitySearchHit::id).toList())
                    .stream()
                    .filter(Opportunity::isPubliclyAvailable)
                    .forEach(opportunity -> loadedById.put(opportunity.getId(), opportunity));
        }

        List<OpportunitySummaryResponse> items = new ArrayList<>();
        OpportunitySearchHit lastDeliveredHit = null;
        for (OpportunitySearchHit hit : pageHits) {
            Opportunity opportunity = loadedById.get(hit.id());
            if (opportunity != null) {
                items.add(mapper.toSummary(opportunity));
                lastDeliveredHit = hit;
            }
        }

        String nextCursor = null;
        if (hasNext && !pageHits.isEmpty()) {
            OpportunitySearchHit cursorHit = lastDeliveredHit == null
                    ? pageHits.getLast()
                    : lastDeliveredHit;
            nextCursor = cursorCodec.encode(
                    new OpportunityCursor(cursorHit.rank(), cursorHit.createdAt(), cursorHit.id()), filter);
        }
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

    private void validateModality(String modality) {
        if (catalogs != null && !catalogs.existsByCategoryAndCodeAndActiveTrue(
                CatalogAdministrationService.OPPORTUNITY_MODALITY, modality))
            throw new BadRequestException("Opportunity modality is not active in the catalog");
    }

    private void validateRegistrationStart(java.time.Instant startsAt) {
        if (startsAt == null) throw new BadRequestException("Registration start is required");
        // Date inputs from browsers are commonly serialized as midnight UTC; preserve that chosen calendar date.
        LocalDate startDate = startsAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        if (startDate.isBefore(today))
            throw new BadRequestException("Registration start must be today or a future date");
    }
}
