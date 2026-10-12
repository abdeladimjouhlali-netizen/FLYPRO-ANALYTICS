package com.flypro.analytics.department.dto;

import java.time.Instant;

/**
 * Search / filter criteria for department listing.
 */
public record DepartmentSearchCriteria(
        String name,
        String code,
        Boolean active,
        Instant createdFrom,
        Instant createdTo
) {
}
