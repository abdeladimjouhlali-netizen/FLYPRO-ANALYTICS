package com.flypro.analytics.alert.dto;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Alert details returned by the API.
 */
@Schema(description = "Alert response")
public record AlertResponseDTO(

        @Schema(example = "1")
        Long id,

        @Schema(example = "Access denied")
        String title,

        String description,

        @Schema(example = "HIGH")
        AlertSeverity severity,

        @Schema(example = "OPEN")
        AlertStatus status,

        @Schema(example = "ACCESS_DENIED")
        AlertType type,

        Instant createdDate,

        Instant resolvedDate,

        @Schema(example = "admin")
        String resolvedBy,

        Long employeeId,

        String employeeNumber,

        String employeeFullName,

        Long badgeId,

        String badgeNumber,

        Long accessEventId,

        Long zoneId,

        String zoneName,

        Long departmentId,

        Instant createdAt
) {
}
