package com.flypro.analytics.zone.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full zone representation returned by the API.
 */
@Schema(description = "Zone details")
public record ZoneResponseDTO(
        Long id,
        String name,
        String building,
        String floor,
        boolean sensitive,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
