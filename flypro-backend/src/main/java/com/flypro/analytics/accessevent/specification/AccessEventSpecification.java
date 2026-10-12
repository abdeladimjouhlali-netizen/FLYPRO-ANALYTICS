package com.flypro.analytics.accessevent.specification;

import com.flypro.analytics.accessevent.dto.AccessEventSearchCriteria;
import com.flypro.analytics.accessevent.entity.AccessEvent;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for access event search.
 */
public final class AccessEventSpecification {

    private AccessEventSpecification() {
    }

    public static Specification<AccessEvent> withCriteria(AccessEventSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            boolean needBadge = StringUtils.hasText(criteria.badgeNumber())
                    || StringUtils.hasText(criteria.keyword());
            boolean needEmployee = criteria.employeeId() != null
                    || criteria.departmentId() != null
                    || StringUtils.hasText(criteria.keyword());
            boolean needAccessPoint = criteria.accessPointId() != null
                    || StringUtils.hasText(criteria.keyword());
            boolean needZone = criteria.zoneId() != null
                    || StringUtils.hasText(criteria.keyword());

            Join<?, ?> badge = needBadge ? root.join("badge", JoinType.LEFT) : null;
            Join<?, ?> employee = needEmployee ? root.join("employee", JoinType.LEFT) : null;
            Join<?, ?> accessPoint = needAccessPoint ? root.join("accessPoint", JoinType.LEFT) : null;
            Join<?, ?> zone = needZone ? root.join("zone", JoinType.LEFT) : null;

            if (StringUtils.hasText(criteria.badgeNumber())) {
                predicates.add(cb.like(
                        cb.upper(badge.get("badgeNumber")),
                        "%" + criteria.badgeNumber().trim().toUpperCase() + "%"
                ));
            }

            if (criteria.employeeId() != null) {
                predicates.add(cb.equal(employee.get("id"), criteria.employeeId()));
            }
            if (criteria.departmentId() != null) {
                predicates.add(cb.equal(employee.get("department").get("id"), criteria.departmentId()));
            }

            if (criteria.zoneId() != null) {
                predicates.add(cb.equal(zone.get("id"), criteria.zoneId()));
            }

            if (criteria.accessPointId() != null) {
                predicates.add(cb.equal(accessPoint.get("id"), criteria.accessPointId()));
            }

            if (criteria.result() != null) {
                predicates.add(cb.equal(root.get("result"), criteria.result()));
            }

            if (criteria.direction() != null) {
                predicates.add(cb.equal(root.get("direction"), criteria.direction()));
            }

            if (criteria.source() != null) {
                predicates.add(cb.equal(root.get("source"), criteria.source()));
            }

            if (criteria.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventTimestamp"), criteria.from()));
            }

            if (criteria.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventTimestamp"), criteria.to()));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(badge.get("badgeNumber")), pattern),
                        cb.like(cb.lower(employee.get("employeeNumber")), pattern),
                        cb.like(cb.lower(employee.get("firstName")), pattern),
                        cb.like(cb.lower(employee.get("lastName")), pattern),
                        cb.like(cb.lower(accessPoint.get("code")), pattern),
                        cb.like(cb.lower(accessPoint.get("name")), pattern),
                        cb.like(cb.lower(zone.get("name")), pattern),
                        cb.like(cb.lower(root.get("refusalReason")), pattern)
                ));
            }

            if (query != null && AccessEvent.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
