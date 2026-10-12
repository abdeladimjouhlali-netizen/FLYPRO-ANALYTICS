package com.flypro.analytics.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight department representation for lists and lookups.
 */
@Schema(description = "Department summary")
public record DepartmentSummaryResponse(
        Long id,
        String name,
        String code,
        boolean active
) {
}
