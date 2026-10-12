package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Chart payload with categories and one or more series.
 */
@Schema(description = "Dashboard chart")
public record ChartDTO(

        @Schema(example = "dailyAccessEvents")
        String key,

        @Schema(example = "Daily Access Events")
        String title,

        @Schema(example = "[\"2026-07-20\",\"2026-07-21\"]")
        List<String> labels,

        List<ChartSeriesDTO> series
) {
}
