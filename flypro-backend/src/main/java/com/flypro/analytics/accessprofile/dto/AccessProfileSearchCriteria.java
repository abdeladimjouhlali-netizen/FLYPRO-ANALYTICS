package com.flypro.analytics.accessprofile.dto;

import com.flypro.analytics.common.enums.SecurityLevel;

/**
 * Search / filter criteria for access profiles.
 */
public record AccessProfileSearchCriteria(
        String name,
        SecurityLevel securityLevel,
        Long zoneId,
        String keyword
) {
}
