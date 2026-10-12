package com.flypro.analytics.accessevent.dto;

import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.zone.dto.ZoneSummaryDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full access event representation returned by the API.
 */
@Schema(description = "Access event details")
public record AccessEventResponseDTO(
        Long id,
        Instant eventTimestamp,
        AccessDirection direction,
        AccessResult result,
        String refusalReason,
        EventSource source,
        String rawPayload,
        Long badgeId,
        String badgeNumber,
        BadgeStatus badgeStatus,
        Long employeeId,
        String employeeNumber,
        String employeeFirstName,
        String employeeLastName,
        Long accessPointId,
        String accessPointCode,
        String accessPointName,
        ZoneSummaryDTO zone,
        Instant createdAt
) {
}
