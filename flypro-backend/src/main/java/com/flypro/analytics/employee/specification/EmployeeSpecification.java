package com.flypro.analytics.employee.specification;

import com.flypro.analytics.employee.dto.EmployeeSearchCriteria;
import com.flypro.analytics.employee.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for employee search.
 */
public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> withCriteria(EmployeeSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.employeeNumber())) {
                predicates.add(cb.like(
                        cb.upper(root.get("employeeNumber")),
                        "%" + criteria.employeeNumber().trim().toUpperCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.firstName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("firstName")),
                        "%" + criteria.firstName().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.lastName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("lastName")),
                        "%" + criteria.lastName().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), criteria.departmentId()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (StringUtils.hasText(criteria.position())) {
                predicates.add(cb.like(
                        cb.lower(root.get("position")),
                        "%" + criteria.position().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.email())) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + criteria.email().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeNumber")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("position")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
