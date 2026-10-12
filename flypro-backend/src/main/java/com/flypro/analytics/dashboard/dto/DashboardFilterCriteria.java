package com.flypro.analytics.dashboard.dto;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;

import java.time.Instant;

/**
 * Shared filter criteria for dashboard analytics endpoints.
 */
public record DashboardFilterCriteria(
        Instant from,
        Instant to,
        Long departmentId,
        Long zoneId,
        Long accessPointId,
        Long employeeId,
        Long badgeId,
        AlertSeverity alertSeverity,
        AlertType alertType
) {
}
