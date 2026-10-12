package com.flypro.analytics.dashboard.service;

import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.dashboard.dto.DashboardFilterCriteria;
import com.flypro.analytics.dashboard.dto.KpiDTO;
import com.flypro.analytics.dashboard.dto.NamedCountDTO;
import com.flypro.analytics.dashboard.dto.StatisticsDTO;
import com.flypro.analytics.dashboard.dto.TrendDTO;
import com.flypro.analytics.dashboard.exception.InvalidDashboardFilterException;
import com.flypro.analytics.dashboard.mapper.DashboardMapper;
import com.flypro.analytics.dashboard.repository.DashboardRepository;
import com.flypro.analytics.dashboard.repository.LabelCountProjection;
import com.flypro.analytics.dashboard.repository.NameCountProjection;
import com.flypro.analytics.dashboard.validator.DashboardFilterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardRepository dashboardRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                dashboardRepository,
                new DashboardMapper() {},
                new DashboardFilterValidator()
        );
    }

    @Test
    @DisplayName("getKpis returns inventory and activity metrics")
    void getKpis() {
        when(dashboardRepository.countEmployees(isNull())).thenReturn(10L);
        when(dashboardRepository.countEmployees(eq(EmployeeStatus.ACTIVE), isNull())).thenReturn(7L);
        when(dashboardRepository.countEmployees(eq(EmployeeStatus.INACTIVE), isNull())).thenReturn(2L);
        when(dashboardRepository.countEmployees(eq(EmployeeStatus.ARCHIVED), isNull())).thenReturn(1L);
        when(dashboardRepository.countDepartments()).thenReturn(3L);
        when(dashboardRepository.countBadges()).thenReturn(12L);
        when(dashboardRepository.countBadges(BadgeStatus.ACTIVE)).thenReturn(8L);
        when(dashboardRepository.countBadges(BadgeStatus.EXPIRED)).thenReturn(1L);
        when(dashboardRepository.countBadges(BadgeStatus.SUSPENDED)).thenReturn(2L);
        when(dashboardRepository.countBadges(BadgeStatus.REVOKED)).thenReturn(1L);
        when(dashboardRepository.countAccessEvents(any())).thenReturn(100L);
        when(dashboardRepository.countAccessEvents(any(), eq(AccessResult.AUTHORIZED))).thenReturn(80L);
        when(dashboardRepository.countAccessEvents(any(), eq(AccessResult.DENIED))).thenReturn(20L);
        when(dashboardRepository.countAccessPoints(isNull())).thenReturn(5L);
        when(dashboardRepository.countZones(isNull())).thenReturn(4L);
        when(dashboardRepository.countZones(true)).thenReturn(1L);
        when(dashboardRepository.countAlerts(eq(AlertStatus.OPEN), any())).thenReturn(6L);
        when(dashboardRepository.countAlerts(eq(AlertStatus.RESOLVED), any())).thenReturn(3L);

        List<KpiDTO> kpis = dashboardService.getKpis(emptyFilter());

        assertThat(kpis).extracting(KpiDTO::key)
                .contains(
                        "totalEmployees", "activeEmployees", "grantedAccesses", "deniedAccesses",
                        "openAlerts", "sensitiveZones"
                );
        assertThat(find(kpis, "totalEmployees").value()).isEqualTo(10L);
        assertThat(find(kpis, "grantedAccesses").value()).isEqualTo(80L);
    }

    @Test
    @DisplayName("invalid date range is rejected")
    void invalidDateRange() {
        DashboardFilterCriteria filter = new DashboardFilterCriteria(
                Instant.parse("2026-07-22T00:00:00Z"),
                Instant.parse("2026-07-20T00:00:00Z"),
                null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> dashboardService.getKpis(filter))
                .isInstanceOf(InvalidDashboardFilterException.class);
    }

    @Test
    @DisplayName("getCharts builds daily and distribution charts")
    void getCharts() {
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        byDay.put(LocalDate.of(2026, 7, 20), 5L);
        byDay.put(LocalDate.of(2026, 7, 21), 8L);
        when(dashboardRepository.countAccessEventsByDay(any())).thenReturn(byDay);
        when(dashboardRepository.countAccessesByDepartment(any(), anyInt())).thenReturn(List.of(named(1L, "IT", 5L)));
        when(dashboardRepository.countAccessesByZone(any(), anyInt())).thenReturn(List.of(named(2L, "Lobby", 4L)));
        when(dashboardRepository.countAccessesByAccessPoint(any(), anyInt())).thenReturn(List.of(named(3L, "Door", 3L)));
        Map<AccessResult, Long> byResult = new EnumMap<>(AccessResult.class);
        byResult.put(AccessResult.AUTHORIZED, 10L);
        byResult.put(AccessResult.DENIED, 2L);
        when(dashboardRepository.countAccessEventsByResult(any())).thenReturn(byResult);
        when(dashboardRepository.countBadgesByStatus()).thenReturn(List.of(label("ACTIVE", 8L)));
        when(dashboardRepository.countEmployeesByStatus(isNull())).thenReturn(List.of(label("ACTIVE", 7L)));
        when(dashboardRepository.countAlertsBySeverity(any())).thenReturn(List.of(label("HIGH", 1L)));
        when(dashboardRepository.countAlertsByType(any())).thenReturn(List.of(label("ACCESS_DENIED", 1L)));

        var charts = dashboardService.getCharts(emptyFilter());

        assertThat(charts).extracting(c -> c.key())
                .contains(
                        "dailyAccessEvents", "weeklyAccessEvents", "monthlyAccessEvents",
                        "grantedVsDenied", "badgeStatusDistribution"
                );
        assertThat(charts.stream().filter(c -> c.key().equals("dailyAccessEvents")).findFirst().orElseThrow()
                .series().getFirst().data()).containsExactly(5L, 8L);
    }

    @Test
    @DisplayName("getStatistics aggregates rankings")
    void getStatistics() {
        when(dashboardRepository.topAccessPoints(any(), anyInt())).thenReturn(List.of(named(1L, "Door A", 9L)));
        when(dashboardRepository.topZones(any(), anyInt())).thenReturn(List.of(named(2L, "Zone A", 7L)));
        when(dashboardRepository.topEmployees(any(), anyInt())).thenReturn(List.of(named(3L, "John Doe", 6L)));
        when(dashboardRepository.employeesWithDeniedAccesses(any(), anyInt())).thenReturn(List.of());
        when(dashboardRepository.badgesExpiringSoon(anyInt(), anyInt())).thenReturn(List.of());
        when(dashboardRepository.departmentsWithMostAccesses(any(), anyInt())).thenReturn(List.of());
        when(dashboardRepository.mostFrequentAlertTypes(any(), anyInt())).thenReturn(List.of());

        StatisticsDTO stats = dashboardService.getStatistics(emptyFilter());

        assertThat(stats.topAccessPoints()).hasSize(1);
        assertThat(stats.topAccessPoints().getFirst().name()).isEqualTo("Door A");
        assertThat(stats.mostActiveEmployees().getFirst().count()).isEqualTo(6L);
    }

    @Test
    @DisplayName("getTrends compares current vs previous period")
    void getTrends() {
        when(dashboardRepository.countAccessEvents(any())).thenReturn(20L, 10L);
        when(dashboardRepository.countAccessEvents(any(), eq(AccessResult.AUTHORIZED))).thenReturn(15L, 8L);
        when(dashboardRepository.countAccessEvents(any(), eq(AccessResult.DENIED))).thenReturn(5L, 2L);
        when(dashboardRepository.countAlerts(eq(AlertStatus.OPEN), any())).thenReturn(3L, 1L);

        DashboardFilterCriteria filter = new DashboardFilterCriteria(
                Instant.parse("2026-07-15T00:00:00Z"),
                Instant.parse("2026-07-22T00:00:00Z"),
                null, null, null, null, null, null, null
        );

        List<TrendDTO> trends = dashboardService.getTrends(filter);

        assertThat(trends).isNotEmpty();
        TrendDTO events = trends.stream().filter(t -> t.key().equals("accessEvents")).findFirst().orElseThrow();
        assertThat(events.currentValue()).isEqualTo(20L);
        assertThat(events.previousValue()).isEqualTo(10L);
        assertThat(events.changePercent()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getTopEmployees respects limit")
    void getTopEmployees() {
        when(dashboardRepository.topEmployees(any(), eq(5)))
                .thenReturn(List.of(named(1L, "A B", 3L)));

        List<NamedCountDTO> top = dashboardService.getTopEmployees(emptyFilter(), 5);

        assertThat(top).hasSize(1);
        assertThat(top.getFirst().name()).isEqualTo("A B");
    }

    @Test
    @DisplayName("getPeakHours aggregates UTC hours")
    void getPeakHours() {
        when(dashboardRepository.findAccessEventTimestamps(any())).thenReturn(List.of(
                Instant.parse("2026-07-20T08:15:00Z"),
                Instant.parse("2026-07-20T08:45:00Z"),
                Instant.parse("2026-07-20T14:00:00Z")
        ));

        List<NamedCountDTO> hours = dashboardService.getPeakHours(emptyFilter());

        assertThat(hours).hasSize(24);
        assertThat(hours.get(8).count()).isEqualTo(2L);
        assertThat(hours.get(14).count()).isEqualTo(1L);
        assertThat(hours.get(0).count()).isZero();
    }

    private static DashboardFilterCriteria emptyFilter() {
        return new DashboardFilterCriteria(null, null, null, null, null, null, null, null, null);
    }

    private static KpiDTO find(List<KpiDTO> kpis, String key) {
        return kpis.stream().filter(k -> k.key().equals(key)).findFirst().orElseThrow();
    }

    private static NameCountProjection named(Long id, String name, Long count) {
        return new NameCountProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }

    private static LabelCountProjection label(String label, Long count) {
        return new LabelCountProjection() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
