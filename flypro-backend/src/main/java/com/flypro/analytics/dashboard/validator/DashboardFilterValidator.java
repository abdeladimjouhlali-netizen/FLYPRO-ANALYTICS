package com.flypro.analytics.dashboard.validator;

import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.exception.InvalidDashboardFilterException;
import org.springframework.stereotype.Component;

/**
 * Validates dashboard filter parameters.
 */
@Component
public class DashboardFilterValidator {

    public void validate(DashboardFilterCriteria filter) {
        if (filter == null) {
            return;
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new InvalidDashboardFilterException("'from' must be before or equal to 'to'");
        }
        if (filter.departmentId() != null && filter.departmentId() <= 0) {
            throw new InvalidDashboardFilterException("departmentId must be a positive identifier");
        }
        if (filter.zoneId() != null && filter.zoneId() <= 0) {
            throw new InvalidDashboardFilterException("zoneId must be a positive identifier");
        }
        if (filter.accessPointId() != null && filter.accessPointId() <= 0) {
            throw new InvalidDashboardFilterException("accessPointId must be a positive identifier");
        }
        if (filter.employeeId() != null && filter.employeeId() <= 0) {
            throw new InvalidDashboardFilterException("employeeId must be a positive identifier");
        }
        if (filter.badgeId() != null && filter.badgeId() <= 0) {
            throw new InvalidDashboardFilterException("badgeId must be a positive identifier");
        }
    }
}
