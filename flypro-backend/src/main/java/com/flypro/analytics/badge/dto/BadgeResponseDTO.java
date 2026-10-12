package com.flypro.analytics.badge.dto;

import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Full badge representation returned by the API.
 */
@Schema(description = "Badge details")
public record BadgeResponseDTO(
        Long id,
        String badgeNumber,
        BadgeType type,
        BadgeStatus status,
        LocalDate issueDate,
        LocalDate expirationDate,
        BadgeAssignmentDTO currentAssignment,
        Instant createdAt,
        Instant updatedAt
) {
}
