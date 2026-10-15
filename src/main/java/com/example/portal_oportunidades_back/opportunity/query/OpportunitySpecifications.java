package com.example.portal_oportunidades_back.opportunity.query;

import com.example.portal_oportunidades_back.opportunity.entity.Opportunity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OpportunitySpecifications {
    private OpportunitySpecifications() { }

    public static Specification<Opportunity> matching(OpportunityFilter filter, OpportunityCursor cursor) {
        return (root, query, builder) -> {
            if (query != null && query.getResultType() == Opportunity.class) {
                root.fetch("recruiter", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isNull(root.get("deletedAt")));
            if (filter.title() != null) {
                predicates.add(builder.like(builder.lower(root.get("title")),
                        "%" + escapeLike(filter.title()) + "%", '\\'));
            }
            if (filter.modality() != null) predicates.add(builder.equal(root.get("modality"), filter.modality()));
            if (filter.status() != null) predicates.add(builder.equal(root.get("status"), filter.status()));
            if (filter.recruiterId() != null) {
                predicates.add(builder.equal(root.get("recruiter").get("id"), filter.recruiterId()));
            }
            if (cursor != null) {
                predicates.add(builder.or(
                        builder.lessThan(root.<Instant>get("createdAt"), cursor.createdAt()),
                        builder.and(builder.equal(root.get("createdAt"), cursor.createdAt()),
                                builder.lessThan(root.<Long>get("id"), cursor.id()))));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
