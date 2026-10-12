package com.flypro.analytics.accessevent.dto;

import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.EventSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Payload to record a new access event.
 */
@Schema(description = "Access event create request")
public record AccessEventRequestDTO(

        @NotNull(message = "Event timestamp is required")
        @Schema(example = "2026-07-26T10:15:30Z")
        Instant eventTimestamp,

        @NotNull(message = "Direction is required")
        @Schema(example = "ENTRY")
        AccessDirection direction,

        @NotNull(message = "Result is required")
        @Schema(example = "AUTHORIZED", description = "May be overridden by business rules based on badge status")
        AccessResult result,

        @Size(max = 500)
        @Schema(example = "Badge suspended")
        String refusalReason,

        @NotNull(message = "Source is required")
        @Schema(example = "API")
        EventSource source,

        @Schema(example = "{\"readerId\":\"R-01\"}")
        String rawPayload,

        @NotNull(message = "Badge id is required")
        @Schema(example = "1")
        Long badgeId,

        @NotNull(message = "Access point id is required")
        @Schema(example = "1")
        Long accessPointId,

        @Schema(example = "1", description = "Optional; when omitted, zone is taken from the access point. When provided, must match the access point zone.")
        Long zoneId
) {
}
