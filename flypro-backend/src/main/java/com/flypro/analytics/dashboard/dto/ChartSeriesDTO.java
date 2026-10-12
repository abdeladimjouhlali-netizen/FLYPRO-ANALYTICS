package com.flypro.analytics.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * One numeric series within a chart.
 */
@Schema(description = "Chart series")
public record ChartSeriesDTO(

        @Schema(example = "AUTHORIZED")
        String name,

        @Schema(example = "[10, 15, 8]")
        List<Long> data
) {
}
