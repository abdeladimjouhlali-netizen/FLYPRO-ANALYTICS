package com.flypro.analytics.accessevent.dto;

import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.EventSource;

import java.time.Instant;

/**
 * Search / filter criteria for access events.
 */
public record AccessEventSearchCriteria(
        String badgeNumber,
        Long employeeId,
        Long departmentId,
        Long zoneId,
        Long accessPointId,
        AccessResult result,
        AccessDirection direction,
        EventSource source,
        Instant from,
        Instant to,
        String keyword
) {
}
