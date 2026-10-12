package com.flypro.analytics.importbatch.specification;

import com.flypro.analytics.importbatch.dto.ImportSearchCriteria;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for import batch search.
 */
public final class ImportBatchSpecification {

    private ImportBatchSpecification() {
    }

    public static Specification<ImportBatch> withCriteria(ImportSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (StringUtils.hasText(criteria.fileName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("fileName")),
                        "%" + criteria.fileName().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.importDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.importDateFrom()));
            }

            if (criteria.importDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.importDateTo()));
            }

            if (StringUtils.hasText(criteria.createdBy())) {
                predicates.add(cb.like(
                        cb.lower(root.get("createdBy")),
                        "%" + criteria.createdBy().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fileName")), pattern),
                        cb.like(cb.lower(root.get("createdBy")), pattern),
                        cb.like(cb.lower(root.get("source")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
