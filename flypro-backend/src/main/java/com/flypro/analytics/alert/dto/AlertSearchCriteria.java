package com.flypro.analytics.alert.dto;

import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;

import java.time.Instant;

/**
 * Search / filter criteria for alerts.
 */
public record AlertSearchCriteria(
        AlertStatus status,
        AlertSeverity severity,
        AlertType type,
        Long employeeId,
        Long badgeId,
        Long zoneId,
        Long departmentId,
        Instant createdFrom,
        Instant createdTo,
        String keyword
) {
}
