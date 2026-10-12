package com.flypro.analytics.accesspoint.dto;

import com.flypro.analytics.common.enums.AccessPointStatus;
import com.flypro.analytics.common.enums.AccessPointType;

/**
 * Search / filter criteria for access points.
 */
public record AccessPointSearchCriteria(
        String name,
        String code,
        AccessPointType type,
        AccessPointStatus status,
        Long zoneId,
        String building,
        String floor,
        String keyword
) {
}
