package com.flypro.analytics.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload to renew a badge (extend validity).
 */
@Schema(description = "Badge renew request")
public record BadgeRenewRequestDTO(

        @NotNull(message = "New expiration date is required")
        @Schema(example = "2028-01-15")
        LocalDate expirationDate,

        @Schema(example = "2026-07-24", description = "Optional new issue date; defaults to today when omitted")
        LocalDate issueDate
) {
}
