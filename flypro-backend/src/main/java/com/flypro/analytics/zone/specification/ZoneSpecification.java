package com.flypro.analytics.zone.specification;

import com.flypro.analytics.zone.dto.ZoneSearchCriteria;
import com.flypro.analytics.zone.entity.Zone;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specifications for zone search.
 */
public final class ZoneSpecification {

    private ZoneSpecification() {
    }

    public static Specification<Zone> withCriteria(ZoneSearchCriteria criteria) {
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

            if (StringUtils.hasText(criteria.building())) {
                predicates.add(cb.like(
                        cb.lower(root.get("building")),
                        "%" + criteria.building().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.floor())) {
                predicates.add(cb.like(
                        cb.lower(root.get("floor")),
                        "%" + criteria.floor().trim().toLowerCase() + "%"
                ));
            }

            if (criteria.sensitive() != null) {
                predicates.add(cb.equal(root.get("sensitive"), criteria.sensitive()));
            }

            if (StringUtils.hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("building")), pattern),
                        cb.like(cb.lower(root.get("floor")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
