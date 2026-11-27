package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.opportunity.query.BooleanSearchQueryParser;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import java.util.List;

public interface OpportunitySearchRepository {
    List<OpportunitySearchHit> searchPublic(OpportunityFilter filter,
            BooleanSearchQueryParser.SearchNode searchQuery, OpportunityCursor cursor, int limit);
}
