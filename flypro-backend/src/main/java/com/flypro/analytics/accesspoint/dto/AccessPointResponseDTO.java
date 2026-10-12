package com.flypro.analytics.accesspoint.dto;

import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;
import com.flypro.analytics.zone.dto.ZoneSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full access point representation returned by the API.
 */
@Schema(description = "Access point details")
public record AccessPointResponseDTO(
        Long id,
        String name,
        String code,
        AccessPointType type,
        String description,
        AccessPointStatus status,
        String ipAddress,
        String location,
        ZoneSummaryDTO zone,
        Instant createdAt,
        Instant updatedAt
) {
}
