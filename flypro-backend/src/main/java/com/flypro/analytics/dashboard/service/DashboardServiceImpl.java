package com.flypro.analytics.dashboard.service;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.dashboard.dto.ChartDTO;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;
import com.flypro.analytics.dashboard.mapper.DashboardMapper;
import com.flypro.analytics.dashboard.repository.DashboardRepository;
import com.flypro.analytics.dashboard.repository.LabelCountProjection;
import com.flypro.analytics.dashboard.validator.DashboardFilterValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only dashboard analytics orchestration.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int DEFAULT_TOP_LIMIT = 10;
    private static final int DEFAULT_CHART_TOP = 10;
    private static final int BADGES_EXPIRING_DAYS = 30;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DashboardRepository dashboardRepository;
    private final DashboardMapper dashboardMapper;
    private final DashboardFilterValidator filterValidator;

    public DashboardServiceImpl(
            DashboardRepository dashboardRepository,
            DashboardMapper dashboardMapper,
            DashboardFilterValidator filterValidator
    ) {
        this.dashboardRepository = dashboardRepository;
        this.dashboardMapper = dashboardMapper;
        this.filterValidator = filterValidator;
    }

    @Override
    public List<KpiDTO> getKpis(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = validated(filter);
        Long departmentId = f.departmentId();
        Long zoneId = f.zoneId();

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfToday = startOfToday.plus(1, ChronoUnit.DAYS).minusMillis(1);
        DashboardFilterCriteria todayFilter = withDateRange(f, startOfToday, endOfToday);

        List<KpiDTO> kpis = new ArrayList<>();
        kpis.add(dashboardMapper.toKpi("totalEmployees", "Total Employees",
                dashboardRepository.countEmployees(departmentId)));
        kpis.add(dashboardMapper.toKpi("activeEmployees", "Active Employees",
                dashboardRepository.countEmployees(EmployeeStatus.ACTIVE, departmentId)));
        kpis.add(dashboardMapper.toKpi("inactiveEmployees", "Inactive Employees",
                dashboardRepository.countEmployees(EmployeeStatus.INACTIVE, departmentId)));
        kpis.add(dashboardMapper.toKpi("archivedEmployees", "Archived Employees",
                dashboardRepository.countEmployees(EmployeeStatus.ARCHIVED, departmentId)));
        kpis.add(dashboardMapper.toKpi("totalDepartments", "Total Departments",
                dashboardRepository.countDepartments()));
        kpis.add(dashboardMapper.toKpi("totalBadges", "Total Badges",
                dashboardRepository.countBadges()));
        kpis.add(dashboardMapper.toKpi("activeBadges", "Active Badges",
                dashboardRepository.countBadges(BadgeStatus.ACTIVE)));
        kpis.add(dashboardMapper.toKpi("expiredBadges", "Expired Badges",
                dashboardRepository.countBadges(BadgeStatus.EXPIRED)));
        kpis.add(dashboardMapper.toKpi("suspendedBadges", "Suspended Badges",
                dashboardRepository.countBadges(BadgeStatus.SUSPENDED)));
        kpis.add(dashboardMapper.toKpi("revokedBadges", "Revoked Badges",
                dashboardRepository.countBadges(BadgeStatus.REVOKED)));
        kpis.add(dashboardMapper.toKpi("totalAccessEvents", "Total Access Events",
                dashboardRepository.countAccessEvents(f)));
        kpis.add(dashboardMapper.toKpi("todaysAccessEvents", "Today's Access Events",
                dashboardRepository.countAccessEvents(todayFilter)));
        kpis.add(dashboardMapper.toKpi("grantedAccesses", "Granted Accesses",
                dashboardRepository.countAccessEvents(f, AccessResult.AUTHORIZED)));
        kpis.add(dashboardMapper.toKpi("deniedAccesses", "Denied Accesses",
                dashboardRepository.countAccessEvents(f, AccessResult.DENIED)));
        kpis.add(dashboardMapper.toKpi("totalAccessPoints", "Total Access Points",
                dashboardRepository.countAccessPoints(zoneId)));
        kpis.add(dashboardMapper.toKpi("totalZones", "Total Zones",
                dashboardRepository.countZones(null)));
        kpis.add(dashboardMapper.toKpi("sensitiveZones", "Sensitive Zones",
                dashboardRepository.countZones(true)));
        kpis.add(dashboardMapper.toKpi("openAlerts", "Open Alerts",
                dashboardRepository.countAlerts(AlertStatus.OPEN, f)));
        kpis.add(dashboardMapper.toKpi("resolvedAlerts", "Resolved Alerts",
                dashboardRepository.countAlerts(AlertStatus.RESOLVED, f)));
        return kpis;
    }

    @Override
    public List<ChartDTO> getCharts(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = validated(filter);
        Map<LocalDate, Long> byDay = dashboardRepository.countAccessEventsByDay(f);

        List<ChartDTO> charts = new ArrayList<>();
        charts.add(buildDailyChart(byDay, f));
        charts.add(buildWeeklyChart(byDay));
        charts.add(buildMonthlyChart(byDay));
        charts.add(dashboardMapper.toDistributionChart(
                "accessesByDepartment",
                "Accesses by Department",
                toLabelFromNamed(dashboardRepository.countAccessesByDepartment(f, DEFAULT_CHART_TOP))
        ));
        charts.add(dashboardMapper.toDistributionChart(
                "accessesByZone",
                "Accesses by Zone",
                toLabelFromNamed(dashboardRepository.countAccessesByZone(f, DEFAULT_CHART_TOP))
        ));
        charts.add(dashboardMapper.toDistributionChart(
                "accessesByAccessPoint",
                "Accesses by Access Point",
                toLabelFromNamed(dashboardRepository.countAccessesByAccessPoint(f, DEFAULT_CHART_TOP))
        ));
        charts.add(buildGrantedVsDeniedChart(f));
        charts.add(dashboardMapper.toDistributionChart(
                "badgeStatusDistribution",
                "Badge Status Distribution",
                dashboardRepository.countBadgesByStatus()
        ));
        charts.add(dashboardMapper.toDistributionChart(
                "employeeStatusDistribution",
                "Employee Status Distribution",
                dashboardRepository.countEmployeesByStatus(f.departmentId())
        ));
        charts.add(dashboardMapper.toDistributionChart(
                "alertSeverityDistribution",
                "Alert Severity Distribution",
                dashboardRepository.countAlertsBySeverity(f)
        ));
        charts.add(dashboardMapper.toDistributionChart(
                "alertTypeDistribution",
                "Alert Type Distribution",
                dashboardRepository.countAlertsByType(f)
        ));
        return charts;
    }

    @Override
    public StatisticsDTO getStatistics(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = validated(filter);
        return new StatisticsDTO(
                dashboardMapper.toNamedCounts(dashboardRepository.topAccessPoints(f, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.topZones(f, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.topEmployees(f, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.employeesWithDeniedAccesses(f, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.badgesExpiringSoon(BADGES_EXPIRING_DAYS, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.departmentsWithMostAccesses(f, DEFAULT_TOP_LIMIT)),
                dashboardMapper.toNamedCounts(dashboardRepository.mostFrequentAlertTypes(f, DEFAULT_TOP_LIMIT))
        );
    }

    @Override
    public List<TrendDTO> getTrends(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = validated(filter);
        InstantRange current = resolveCurrentRange(f);
        InstantRange previous = previousRange(current);

        DashboardFilterCriteria currentFilter = withDateRange(f, current.from(), current.to());
        DashboardFilterCriteria previousFilter = withDateRange(f, previous.from(), previous.to());

        long currentEvents = dashboardRepository.countAccessEvents(currentFilter);
        long previousEvents = dashboardRepository.countAccessEvents(previousFilter);
        long currentGranted = dashboardRepository.countAccessEvents(currentFilter, AccessResult.AUTHORIZED);
        long previousGranted = dashboardRepository.countAccessEvents(previousFilter, AccessResult.AUTHORIZED);
        long currentDenied = dashboardRepository.countAccessEvents(currentFilter, AccessResult.DENIED);
        long previousDenied = dashboardRepository.countAccessEvents(previousFilter, AccessResult.DENIED);
        long currentOpenAlerts = dashboardRepository.countAlerts(AlertStatus.OPEN, currentFilter);
        long previousOpenAlerts = dashboardRepository.countAlerts(AlertStatus.OPEN, previousFilter);

        return List.of(
                dashboardMapper.toTrend("accessEvents", "Access Events", currentEvents, previousEvents),
                dashboardMapper.toTrend("grantedAccesses", "Granted Accesses", currentGranted, previousGranted),
                dashboardMapper.toTrend("deniedAccesses", "Denied Accesses", currentDenied, previousDenied),
                dashboardMapper.toTrend("openAlerts", "Open Alerts", currentOpenAlerts, previousOpenAlerts)
        );
    }

    @Override
    public List<NamedCountDTO> getTopAccessPoints(DashboardFilterCriteria filter, int limit) {
        return dashboardMapper.toNamedCounts(
                dashboardRepository.topAccessPoints(validated(filter), normalizeLimit(limit))
        );
    }

    @Override
    public List<NamedCountDTO> getTopZones(DashboardFilterCriteria filter, int limit) {
        return dashboardMapper.toNamedCounts(
                dashboardRepository.topZones(validated(filter), normalizeLimit(limit))
        );
    }

    @Override
    public List<NamedCountDTO> getTopEmployees(DashboardFilterCriteria filter, int limit) {
        return dashboardMapper.toNamedCounts(
                dashboardRepository.topEmployees(validated(filter), normalizeLimit(limit))
        );
    }

    @Override
    public List<NamedCountDTO> getPeakHours(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = validated(filter);
        List<Instant> timestamps = dashboardRepository.findAccessEventTimestamps(f);

        long[] hours = new long[24];
        for (Instant ts : timestamps) {
            if (ts != null) {
                hours[ts.atZone(ZoneOffset.UTC).getHour()]++;
            }
        }

        List<NamedCountDTO> result = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            String label = String.format("%02d:00", h);
            result.add(new NamedCountDTO((long) h, label, hours[h]));
        }
        return result;
    }

    private DashboardFilterCriteria validated(DashboardFilterCriteria filter) {
        DashboardFilterCriteria f = filter == null
                ? new DashboardFilterCriteria(null, null, null, null, null, null, null, null, null)
                : filter;
        filterValidator.validate(f);
        return f;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_TOP_LIMIT;
        }
        return Math.min(limit, 100);
    }

    private ChartDTO buildDailyChart(Map<LocalDate, Long> byDay, DashboardFilterCriteria filter) {
        Map<LocalDate, Long> filled = fillDailyBuckets(byDay, filter);
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        filled.forEach((day, count) -> {
            labels.add(DAY_FMT.format(day));
            values.add(count);
        });
        return dashboardMapper.toSingleSeriesChart("dailyAccessEvents", "Daily Access Events", labels, values);
    }

    private ChartDTO buildWeeklyChart(Map<LocalDate, Long> byDay) {
        Map<LocalDate, Long> byWeek = new TreeMap<>();
        byDay.forEach((day, count) -> {
            LocalDate weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.merge(weekStart, count, Long::sum);
        });
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        byWeek.forEach((week, count) -> {
            labels.add(WEEK_FMT.format(week));
            values.add(count);
        });
        return dashboardMapper.toSingleSeriesChart("weeklyAccessEvents", "Weekly Access Events", labels, values);
    }

    private ChartDTO buildMonthlyChart(Map<LocalDate, Long> byDay) {
        Map<YearMonth, Long> byMonth = new TreeMap<>();
        byDay.forEach((day, count) -> byMonth.merge(YearMonth.from(day), count, Long::sum));
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        byMonth.forEach((month, count) -> {
            labels.add(MONTH_FMT.format(month));
            values.add(count);
        });
        return dashboardMapper.toSingleSeriesChart("monthlyAccessEvents", "Monthly Access Events", labels, values);
    }

    private ChartDTO buildGrantedVsDeniedChart(DashboardFilterCriteria filter) {
        Map<AccessResult, Long> byResult = dashboardRepository.countAccessEventsByResult(filter);
        long granted = byResult.getOrDefault(AccessResult.AUTHORIZED, 0L);
        long denied = byResult.getOrDefault(AccessResult.DENIED, 0L);
        return dashboardMapper.toSingleSeriesChart(
                "grantedVsDenied",
                "Granted vs Denied",
                List.of("AUTHORIZED", "DENIED"),
                List.of(granted, denied)
        );
    }

    private Map<LocalDate, Long> fillDailyBuckets(Map<LocalDate, Long> byDay, DashboardFilterCriteria filter) {
        if (byDay == null || byDay.isEmpty()) {
            return Map.of();
        }
        LocalDate start = filter.from() != null
                ? filter.from().atZone(ZoneOffset.UTC).toLocalDate()
                : byDay.keySet().stream().min(LocalDate::compareTo).orElse(LocalDate.now(ZoneOffset.UTC));
        LocalDate end = filter.to() != null
                ? filter.to().atZone(ZoneOffset.UTC).toLocalDate()
                : byDay.keySet().stream().max(LocalDate::compareTo).orElse(start);

        Map<LocalDate, Long> filled = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            filled.put(d, byDay.getOrDefault(d, 0L));
        }
        return filled;
    }

    private List<LabelCountProjection> toLabelFromNamed(
            List<com.flypro.analytics.dashboard.repository.NameCountProjection> named
    ) {
        List<LabelCountProjection> labels = new ArrayList<>();
        if (named == null) {
            return labels;
        }
        for (var n : named) {
            labels.add(new LabelCountProjection() {
                @Override
                public String getLabel() {
                    return n.getName();
                }

                @Override
                public Long getCount() {
                    return n.getCount();
                }
            });
        }
        return labels;
    }

    private InstantRange resolveCurrentRange(DashboardFilterCriteria filter) {
        if (filter.from() != null && filter.to() != null) {
            return new InstantRange(filter.from(), filter.to());
        }
        Instant to = filter.to() != null ? filter.to() : Instant.now();
        Instant from = filter.from() != null ? filter.from() : to.minus(7, ChronoUnit.DAYS);
        return new InstantRange(from, to);
    }

    private InstantRange previousRange(InstantRange current) {
        long millis = ChronoUnit.MILLIS.between(current.from(), current.to());
        if (millis <= 0) {
            millis = ChronoUnit.DAYS.getDuration().toMillis() * 7;
        }
        Instant previousTo = current.from().minusMillis(1);
        Instant previousFrom = previousTo.minusMillis(millis);
        return new InstantRange(previousFrom, previousTo);
    }

    private DashboardFilterCriteria withDateRange(DashboardFilterCriteria base, Instant from, Instant to) {
        return new DashboardFilterCriteria(
                from,
                to,
                base.departmentId(),
                base.zoneId(),
                base.accessPointId(),
                base.employeeId(),
                base.badgeId(),
                base.alertSeverity(),
                base.alertType()
        );
    }

    private record InstantRange(Instant from, Instant to) {
    }
}
