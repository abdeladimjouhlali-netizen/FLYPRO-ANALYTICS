package com.flypro.analytics.badge.dto;

import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;

import java.time.LocalDate;

/**
 * Search / filter criteria for badges.
 */
public record BadgeSearchCriteria(
        String badgeNumber,
        Long employeeId,
        BadgeStatus status,
        BadgeType type,
        LocalDate expirationDateFrom,
        LocalDate expirationDateTo,
        Long departmentId,
        String keyword
) {
}
