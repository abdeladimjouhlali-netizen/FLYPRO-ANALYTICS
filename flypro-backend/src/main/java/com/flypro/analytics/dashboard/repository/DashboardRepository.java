package com.flypro.analytics.dashboard.repository;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Read-only analytics queries for the dashboard.
 */
public interface DashboardRepository {

    long countEmployees(EmployeeStatus status, Long departmentId);

    long countEmployees(Long departmentId);

    long countDepartments();

    long countBadges(BadgeStatus status);

    long countBadges();

    long countAccessPoints(Long zoneId);

    long countZones(Boolean sensitiveOnly);

    long countAccessEvents(DashboardFilterCriteria filter, AccessResult result);

    long countAccessEvents(DashboardFilterCriteria filter);

    long countAlerts(AlertStatus status, DashboardFilterCriteria filter);

    long countAlerts(DashboardFilterCriteria filter);

    Map<LocalDate, Long> countAccessEventsByDay(DashboardFilterCriteria filter);

    Map<AccessResult, Long> countAccessEventsByResult(DashboardFilterCriteria filter);

    List<NameCountProjection> countAccessesByDepartment(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> countAccessesByZone(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> countAccessesByAccessPoint(DashboardFilterCriteria filter, int limit);

    List<LabelCountProjection> countBadgesByStatus();

    List<LabelCountProjection> countEmployeesByStatus(Long departmentId);

    List<LabelCountProjection> countAlertsBySeverity(DashboardFilterCriteria filter);

    List<LabelCountProjection> countAlertsByType(DashboardFilterCriteria filter);

    List<NameCountProjection> topAccessPoints(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> topZones(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> topEmployees(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> employeesWithDeniedAccesses(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> badgesExpiringSoon(int days, int limit);

    List<NameCountProjection> departmentsWithMostAccesses(DashboardFilterCriteria filter, int limit);

    List<NameCountProjection> mostFrequentAlertTypes(DashboardFilterCriteria filter, int limit);

    /**
     * Returns raw event timestamps for peak-hour aggregation (portable across DBs).
     */
    List<Instant> findAccessEventTimestamps(DashboardFilterCriteria filter);
}
