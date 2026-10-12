package com.flypro.analytics.leave.specification;

import com.flypro.analytics.leave.dto.LeaveRequestSearchCriteria;
import com.flypro.analytics.leave.entity.LeaveRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for leave request search.
 */
public final class LeaveRequestSpecification {

    private LeaveRequestSpecification() {
    }

    public static Specification<LeaveRequest> withCriteria(LeaveRequestSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            boolean needEmployee = criteria.employeeId() != null
                    || criteria.departmentId() != null
                    || StringUtils.hasText(criteria.keyword());

            Join<?, ?> employee = needEmployee ? root.join("employee", JoinType.LEFT) : null;

            if (criteria.employeeId() != null) {
                predicates.add(cb.equal(employee.get("id"), criteria.employeeId()));
            }
            if (criteria.departmentId() != null) {
                predicates.add(cb.equal(employee.get("department").get("id"), criteria.departmentId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }
            if (criteria.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), criteria.to()));
            }
            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(employee.get("employeeNumber")), pattern),
                        cb.like(cb.lower(employee.get("firstName")), pattern),
                        cb.like(cb.lower(employee.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("reason")), pattern)
                ));
            }

            if (query != null && LeaveRequest.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
