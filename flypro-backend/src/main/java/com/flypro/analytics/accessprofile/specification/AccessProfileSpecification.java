package com.flypro.analytics.accessprofile.specification;

import com.flypro.analytics.accessprofile.dto.AccessProfileSearchCriteria;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for access profile search.
 */
public final class AccessProfileSpecification {

    private AccessProfileSpecification() {
    }

    public static Specification<AccessProfile> withCriteria(AccessProfileSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.name())) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + criteria.name().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.securityLevel() != null) {
                predicates.add(cb.equal(root.get("securityLevel"), criteria.securityLevel()));
            }

            if (criteria.zoneId() != null) {
                var zones = root.join("zones", JoinType.INNER);
                predicates.add(cb.equal(zones.get("id"), criteria.zoneId()));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (query != null && AccessProfile.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
