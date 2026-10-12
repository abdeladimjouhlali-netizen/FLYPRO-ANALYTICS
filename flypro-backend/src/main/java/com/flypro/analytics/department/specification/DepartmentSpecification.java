package com.flypro.analytics.department.specification;

import com.flypro.analytics.department.dto.DepartmentSearchCriteria;
import com.flypro.analytics.department.entity.Department;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for department search.
 */
public final class DepartmentSpecification {

    private DepartmentSpecification() {
    }

    public static Specification<Department> withCriteria(DepartmentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (StringUtils.hasText(criteria.name())) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + criteria.name().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.code())) {
                predicates.add(cb.like(
                        cb.upper(root.get("code")),
                        "%" + criteria.code().trim().toUpperCase() + "%"
                ));
            }

            if (criteria.active() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.active()));
            }

            if (criteria.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdFrom()));
            }

            if (criteria.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.createdTo()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
