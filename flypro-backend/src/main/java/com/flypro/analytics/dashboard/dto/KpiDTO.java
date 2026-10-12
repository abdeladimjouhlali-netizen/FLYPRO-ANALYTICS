package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single KPI metric for the dashboard.
 */
@Schema(description = "Dashboard KPI")
public record KpiDTO(

        @Schema(example = "totalEmployees")
        String key,

        @Schema(example = "Total Employees")
        String label,

        @Schema(example = "150")
        long value,

        @Schema(example = "count")
        String unit
) {
}
