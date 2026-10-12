package com.flypro.analytics.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Payload to replace an assigned badge with another existing badge.
 */
@Schema(description = "Badge replace request")
public record BadgeReplaceRequestDTO(

        @NotNull(message = "New badge id is required")
        @Schema(example = "2", description = "Id of the replacement badge to assign to the same employee")
        Long newBadgeId
) {
}
