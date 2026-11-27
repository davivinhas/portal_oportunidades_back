package com.example.portal_oportunidades_back.opportunity.repository;

import com.example.portal_oportunidades_back.exception.BadRequestException;
import com.example.portal_oportunidades_back.opportunity.entity.OpportunityStatus;
import com.example.portal_oportunidades_back.opportunity.query.BooleanSearchQueryParser;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityCursor;
import com.example.portal_oportunidades_back.opportunity.query.OpportunityFilter;
import java.time.Instant;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.val;

@Repository
public class OpportunitySearchRepositoryImpl implements OpportunitySearchRepository {
    private static final String RANK_EXPRESSION =
            "ts_rank_cd(ARRAY[0.1, 0.2, 0.4, 1.0]::real[], {0}, {1}, 32)";

    private final DSLContext dsl;

    public OpportunitySearchRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public List<OpportunitySearchHit> searchPublic(OpportunityFilter filter,
            BooleanSearchQueryParser.SearchNode searchQuery, OpportunityCursor cursor, int limit) {
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("Limit must be between 1 and 100");
        }
        if (filter.status() != null && filter.status() != OpportunityStatus.PUBLISHED) {
            return List.of();
        }
        if (cursor != null && cursor.rank() == null) {
            throw new BadRequestException("Search cursor is missing its relevance rank");
        }

        var opportunity = table(name("oportunidades", "oportunidade")).as("o");
        Field<Long> id = field(name("o", "id"), SQLDataType.BIGINT);
        Field<Instant> createdAt = field(name("o", "criado_em"), SQLDataType.INSTANT);
        Field<String> status = field(name("o", "status"), SQLDataType.VARCHAR);
        Field<String> modality = field(name("o", "modalidade"), SQLDataType.VARCHAR);
        Field<Long> recruiterId = field(name("o", "recrutador_id"), SQLDataType.BIGINT);
        Field<String> location = field(name("o", "localizacao"), SQLDataType.VARCHAR);
        Field<String> area = field(name("o", "area"), SQLDataType.VARCHAR);
        Field<Instant> registrationEndsAt = field(name("o", "fim_inscricoes"), SQLDataType.INSTANT);
        Field<Object> searchVector = field(name("o", "search_vector"), SQLDataType.OTHER);
        Field<Object> tsquery = toTsQuery(searchQuery);
        Field<Float> rank = field(RANK_EXPRESSION, SQLDataType.REAL, searchVector, tsquery);
        Field<Float> selectedRank = rank.as("rank");

        Field<Integer> nodeCount = field("numnode({0})", SQLDataType.INTEGER, tsquery);
        Record1<Integer> nodeCountRecord = dsl.select(nodeCount).fetchOne();
        if (nodeCountRecord == null || nodeCountRecord.value1() == null || nodeCountRecord.value1() == 0) {
            throw new BadRequestException("Search query contains no searchable terms");
        }

        Condition conditions = field(name("o", "deleted_at"), SQLDataType.INSTANT).isNull()
                .and(condition("{0} = CAST({1} AS oportunidades.status_oportunidade)",
                        status, val(OpportunityStatus.PUBLISHED.name())))
                .and(condition("{0} @@ {1}", searchVector, tsquery));

        if (filter.modality() != null) {
            conditions = conditions.and(modality.eq(filter.modality()));
        }
        if (filter.recruiterId() != null) {
            conditions = conditions.and(recruiterId.eq(filter.recruiterId()));
        }
        if (filter.location() != null) conditions = conditions.and(location.likeIgnoreCase("%" + filter.location() + "%"));
        if (filter.area() != null) conditions = conditions.and(area.likeIgnoreCase("%" + filter.area() + "%"));
        if (filter.deadlineFrom() != null) conditions = conditions.and(registrationEndsAt.ge(filter.deadlineFrom()));
        if (filter.deadlineTo() != null) conditions = conditions.and(registrationEndsAt.le(filter.deadlineTo()));
        if (cursor != null) {
            Field<Float> cursorRank = val(cursor.rank().floatValue(), SQLDataType.REAL);
            Field<Instant> cursorCreatedAt = val(cursor.createdAt(), SQLDataType.INSTANT);
            conditions = conditions.and(
                    rank.lt(cursorRank)
                            .or(rank.eq(cursorRank).and(createdAt.lt(cursorCreatedAt)))
                            .or(rank.eq(cursorRank).and(createdAt.eq(cursorCreatedAt))
                                    .and(id.lt(cursor.id()))));
        }

        return dsl.select(id, createdAt, selectedRank)
                .from(opportunity)
                .where(conditions)
                .orderBy(rank.desc(), createdAt.desc(), id.desc())
                .limit(limit + 1)
                .fetch(record -> new OpportunitySearchHit(
                        record.get(id), record.get(createdAt), record.get(selectedRank)));
    }

    private Field<Object> toTsQuery(BooleanSearchQueryParser.SearchNode node) {
        return switch (node) {
            case BooleanSearchQueryParser.Term term -> field(
                    "plainto_tsquery('portuguese', {0})", SQLDataType.OTHER, val(term.value()));
            case BooleanSearchQueryParser.Phrase phrase -> field(
                    "phraseto_tsquery('portuguese', {0})", SQLDataType.OTHER, val(phrase.value()));
            case BooleanSearchQueryParser.Not not -> field(
                    "!!({0})", SQLDataType.OTHER, toTsQuery(not.child()));
            case BooleanSearchQueryParser.And and -> field(
                    "({0} && {1})", SQLDataType.OTHER,
                    toTsQuery(and.left()), toTsQuery(and.right()));
            case BooleanSearchQueryParser.Or or -> field(
                    "({0} || {1})", SQLDataType.OTHER,
                    toTsQuery(or.left()), toTsQuery(or.right()));
        };
    }
}
