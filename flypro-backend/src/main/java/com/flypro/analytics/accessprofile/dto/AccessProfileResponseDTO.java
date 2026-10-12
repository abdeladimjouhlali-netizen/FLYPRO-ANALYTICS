package com.flypro.analytics.accessprofile.dto;

import com.flypro.analytics.common.enums.SecurityLevel;
import com.flypro.analytics.zone.dto.ZoneSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;

/**
 * Full access profile representation returned by the API.
 */
@Schema(description = "Access profile details")
public record AccessProfileResponseDTO(
        Long id,
        String name,
        String description,
        SecurityLevel securityLevel,
        LocalTime startTime,
        LocalTime endTime,
        Set<ZoneSummaryDTO> zones,
        Instant createdAt,
        Instant updatedAt
) {
}
