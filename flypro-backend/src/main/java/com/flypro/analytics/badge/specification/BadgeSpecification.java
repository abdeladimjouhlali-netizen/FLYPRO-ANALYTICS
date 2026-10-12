package com.flypro.analytics.badge.specification;

import com.flypro.analytics.badge.dto.BadgeSearchCriteria;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.badgeassignment.entity.BadgeAssignment;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for badge search.
 */
public final class BadgeSpecification {

    private BadgeSpecification() {
    }

    public static Specification<Badge> withCriteria(BadgeSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.badgeNumber())) {
                predicates.add(cb.like(
                        cb.upper(root.get("badgeNumber")),
                        "%" + criteria.badgeNumber().trim().toUpperCase() + "%"
                ));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }

            if (criteria.expirationDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("expirationDate"), criteria.expirationDateFrom()
                ));
            }

            if (criteria.expirationDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("expirationDate"), criteria.expirationDateTo()
                ));
            }

            if (criteria.employeeId() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BadgeAssignment> assignment = subquery.from(BadgeAssignment.class);
                subquery.select(assignment.get("badge").get("id"))
                        .where(
                                cb.equal(assignment.get("employee").get("id"), criteria.employeeId()),
                                cb.isTrue(assignment.get("active"))
                        );
                predicates.add(root.get("id").in(subquery));
            }

            if (criteria.departmentId() != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BadgeAssignment> assignment = subquery.from(BadgeAssignment.class);
                subquery.select(assignment.get("badge").get("id"))
                        .where(
                                cb.equal(assignment.get("employee").get("department").get("id"),
                                        criteria.departmentId()),
                                cb.isTrue(assignment.get("active"))
                        );
                predicates.add(root.get("id").in(subquery));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";

                Subquery<Long> employeeSub = query.subquery(Long.class);
                Root<BadgeAssignment> ba = employeeSub.from(BadgeAssignment.class);
                Join<?, ?> employee = ba.join("employee", JoinType.INNER);
                employeeSub.select(ba.get("badge").get("id"))
                        .where(cb.or(
                                cb.like(cb.lower(employee.get("employeeNumber")), pattern),
                                cb.like(cb.lower(employee.get("firstName")), pattern),
                                cb.like(cb.lower(employee.get("lastName")), pattern),
                                cb.like(cb.lower(employee.get("email")), pattern)
                        ));

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("badgeNumber")), pattern),
                        root.get("id").in(employeeSub)
                ));
            }

            if (query != null && Badge.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
