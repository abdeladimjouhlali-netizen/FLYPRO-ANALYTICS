package com.flypro.analytics.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full department representation.
 */
@Schema(description = "Department details")
public record DepartmentResponse(
        Long id,
        String name,
        String code,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
