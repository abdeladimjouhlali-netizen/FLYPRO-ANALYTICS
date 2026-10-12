package com.flypro.analytics.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight zone summary embedded in access profile responses.
 */
@Schema(description = "Zone summary")
public record ZoneSummaryDTO(
        Long id,
        String name,
        String building,
        String floor,
        boolean sensitive
) {
}
