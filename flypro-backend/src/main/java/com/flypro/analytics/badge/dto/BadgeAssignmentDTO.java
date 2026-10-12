package com.flypro.analytics.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Badge assignment history entry (never exposes JPA entities).
 */
@Schema(description = "Badge assignment details")
public record BadgeAssignmentDTO(
        Long id,
        Long badgeId,
        String badgeNumber,
        Long employeeId,
        String employeeNumber,
        String employeeFirstName,
        String employeeLastName,
        Instant assignedAt,
        Instant returnedAt,
        boolean active
) {
}
