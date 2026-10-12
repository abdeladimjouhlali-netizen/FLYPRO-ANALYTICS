package com.flypro.analytics.common.enums;

/**
 * Business / operational alert categories.
 * Extensible for future AI-driven alert types.
 */
public enum AlertType {
    BADGE_EXPIRED,
    BADGE_REVOKED,
    BADGE_SUSPENDED,
    ACCESS_DENIED,
    MULTIPLE_ACCESS_DENIED,
    OUTSIDE_WORKING_HOURS,
    UNAUTHORIZED_ZONE,
    UNKNOWN_BADGE,
    INACTIVE_EMPLOYEE,
    SYSTEM_WARNING
}
