package com.flypro.analytics.alert.specification;

import com.flypro.analytics.alert.dto.AlertSearchCriteria;
import com.flypro.analytics.alert.entity.Alert;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for alert search.
 */
public final class AlertSpecification {

    private AlertSpecification() {
    }

    public static Specification<Alert> withCriteria(AlertSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("employee", JoinType.LEFT);
                root.fetch("badge", JoinType.LEFT);
                root.fetch("accessEvent", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (criteria == null) {
                return cb.conjunction();
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.severity() != null) {
                predicates.add(cb.equal(root.get("severity"), criteria.severity()));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.employeeId() != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), criteria.employeeId()));
            }
            if (criteria.badgeId() != null) {
                predicates.add(cb.equal(root.get("badge").get("id"), criteria.badgeId()));
            }
            if (criteria.zoneId() != null) {
                var accessEvent = root.join("accessEvent", JoinType.LEFT);
                predicates.add(cb.equal(accessEvent.get("zone").get("id"), criteria.zoneId()));
            }
            if (criteria.departmentId() != null) {
                var employee = root.join("employee", JoinType.LEFT);
                predicates.add(cb.equal(employee.get("department").get("id"), criteria.departmentId()));
            }
            if (criteria.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), criteria.createdFrom()));
            }
            if (criteria.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), criteria.createdTo()));
            }
            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
