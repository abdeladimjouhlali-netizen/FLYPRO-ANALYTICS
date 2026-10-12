package com.flypro.analytics.accesspoint.specification;

import com.flypro.analytics.accesspoint.dto.AccessPointSearchCriteria;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for access point search.
 */
public final class AccessPointSpecification {

    private AccessPointSpecification() {
    }

    public static Specification<AccessPoint> withCriteria(AccessPointSearchCriteria criteria) {
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

            if (StringUtils.hasText(criteria.code())) {
                predicates.add(cb.like(
                        cb.upper(root.get("code")),
                        "%" + criteria.code().trim().toUpperCase() + "%"
                ));
            }

            if (criteria.type() != null) {
                predicates.add(cb.equal(root.get("type"), criteria.type()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            boolean needsZone = criteria.zoneId() != null
                    || StringUtils.hasText(criteria.building())
                    || StringUtils.hasText(criteria.floor())
                    || StringUtils.hasText(criteria.keyword());

            Join<?, ?> zone = null;
            if (needsZone) {
                zone = root.join("zone", JoinType.INNER);
            }

            if (criteria.zoneId() != null) {
                predicates.add(cb.equal(zone.get("id"), criteria.zoneId()));
            }
            if (StringUtils.hasText(criteria.building())) {
                predicates.add(cb.like(
                        cb.lower(zone.get("building")),
                        "%" + criteria.building().trim().toLowerCase() + "%"
                ));
            }
            if (StringUtils.hasText(criteria.floor())) {
                predicates.add(cb.like(
                        cb.lower(zone.get("floor")),
                        "%" + criteria.floor().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern),
                        cb.like(cb.lower(root.get("ipAddress")), pattern),
                        cb.like(cb.lower(zone.get("name")), pattern),
                        cb.like(cb.lower(zone.get("building")), pattern)
                ));
            }

            if (query != null && AccessPoint.class.equals(query.getResultType())) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
