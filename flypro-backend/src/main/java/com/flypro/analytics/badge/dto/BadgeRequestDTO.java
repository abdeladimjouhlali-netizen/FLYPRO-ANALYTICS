package com.flypro.analytics.badge.dto;

import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Create / update payload for a badge.
 */
@Schema(description = "Badge create/update request")
public record BadgeRequestDTO(

        @NotBlank(message = "Badge number is required")
        @Size(min = 2, max = 100, message = "Badge number must be between 2 and 100 characters")
        @Schema(example = "BDG-001")
        String badgeNumber,

        @NotNull(message = "Badge type is required")
        @Schema(example = "RFID")
        BadgeType type,

        @NotNull(message = "Badge status is required")
        @Schema(example = "PENDING_ACTIVATION")
        BadgeStatus status,

        @NotNull(message = "Issue date is required")
        @Schema(example = "2026-01-15")
        LocalDate issueDate,

        @Schema(example = "2027-01-15", description = "Must be after issue date when provided")
        LocalDate expirationDate
) {
}
