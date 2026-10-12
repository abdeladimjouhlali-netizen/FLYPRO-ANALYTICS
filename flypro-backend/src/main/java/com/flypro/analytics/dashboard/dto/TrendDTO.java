package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Trend point comparing current vs previous period.
 */
@Schema(description = "Trend metric")
public record TrendDTO(

        @Schema(example = "accessEvents")
        String key,

        @Schema(example = "Access Events")
        String label,

        @Schema(example = "120")
        long currentValue,

        @Schema(example = "100")
        long previousValue,

        @Schema(example = "20.0")
        double changePercent
) {
}
