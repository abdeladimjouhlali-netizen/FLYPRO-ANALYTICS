package com.flypro.analytics.alert.engine;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.alert.engine.rules.AccessProfilePolicyRule;
import com.flypro.analytics.alert.engine.rules.BadgeAndAccessDenialRule;
import com.flypro.analytics.alert.engine.rules.EmployeeAndUnknownBadgeRule;
import com.flypro.analytics.alert.engine.rules.MultipleAccessDeniedRule;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AccessDirection;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.enums.BadgeStatus;
import com.flypro.analytics.common.enums.BadgeType;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.common.enums.EventSource;
import com.flypro.analytics.common.enums.SecurityLevel;
import com.flypro.analytics.department.entity.Department;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.zone.entity.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertRuleEngineTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AccessEventRepository accessEventRepository;
    @Mock
    private AccessProfileRepository accessProfileRepository;

    private DefaultAlertRuleEngine engine;

    private Zone zone;
    private Badge badge;
    private Employee employee;
    private AccessEvent event;

    @BeforeEach
    void setUp() {
        zone = Zone.builder().id(10L).name("Secure").building("A").floor("1").sensitive(true).build();
        badge = Badge.builder()
                .id(3L)
                .badgeNumber("BDG-001")
                .status(BadgeStatus.ACTIVE)
                .type(BadgeType.RFID)
                .issueDate(LocalDate.now().minusDays(10))
                .build();
        Department dept = Department.builder().id(1L).name("IT").code("IT").active(true).build();
        AccessProfile profile = AccessProfile.builder()
                .id(7L)
                .name("Std")
                .securityLevel(SecurityLevel.MEDIUM)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .zones(new HashSet<>(Set.of(zone)))
                .build();
        employee = Employee.builder()
                .id(4L)
                .employeeNumber("EMP-1")
                .firstName("A")
                .lastName("B")
                .email("a@b.com")
                .position("Dev")
                .status(EmployeeStatus.ACTIVE)
                .department(dept)
                .accessProfile(profile)
                .build();

        event = AccessEvent.builder()
                .id(100L)
                .eventTimestamp(Instant.parse("2026-07-26T10:00:00Z"))
                .direction(AccessDirection.ENTRY)
                .result(AccessResult.DENIED)
                .source(EventSource.API)
                .badge(badge)
                .employee(employee)
                .zone(zone)
                .build();

        engine = new DefaultAlertRuleEngine(List.of(
                new BadgeAndAccessDenialRule(),
                new MultipleAccessDeniedRule(accessEventRepository),
                new AccessProfilePolicyRule(accessProfileRepository),
                new EmployeeAndUnknownBadgeRule()
        ), alertRepository);

        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(a.getId() == null ? 1L : a.getId());
            return a;
        });
        when(alertRepository.existsByTypeAndAccessEventId(any(), any())).thenReturn(false);
        when(alertRepository.existsByTypeAndBadgeIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(alertRepository.existsByTypeAndEmployeeIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(accessProfileRepository.findByIdWithZones(7L)).thenReturn(java.util.Optional.of(profile));
    }

    @Test
    @DisplayName("creates ACCESS_DENIED alert")
    void accessDenied() {
        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("creates BADGE_REVOKED alert")
    void badgeRevoked() {
        badge.setStatus(BadgeStatus.REVOKED);
        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.BADGE_REVOKED, AlertType.ACCESS_DENIED);
    }

    @Test
    @DisplayName("creates BADGE_SUSPENDED alert")
    void badgeSuspended() {
        badge.setStatus(BadgeStatus.SUSPENDED);
        List<Alert> alerts = engine.evaluateBadgeStatusChange(badge, employee);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.BADGE_SUSPENDED);
    }

    @Test
    @DisplayName("creates BADGE_EXPIRED alert")
    void badgeExpired() {
        badge.setStatus(BadgeStatus.EXPIRED);
        event.setResult(AccessResult.EXPIRED);
        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.BADGE_EXPIRED);
    }

    @Test
    @DisplayName("creates MULTIPLE_ACCESS_DENIED when threshold reached")
    void multipleDenied() {
        when(accessEventRepository.countByBadgeIdAndResultInAndEventTimestampGreaterThanEqual(
                any(), any(), any())).thenReturn(3L);

        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.MULTIPLE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("creates UNAUTHORIZED_ZONE when zone not in profile")
    void unauthorizedZone() {
        Zone other = Zone.builder().id(99L).name("Other").building("B").floor("2").sensitive(false).build();
        event.setZone(other);

        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.UNAUTHORIZED_ZONE);
    }

    @Test
    @DisplayName("creates OUTSIDE_WORKING_HOURS")
    void outsideHours() {
        event.setEventTimestamp(Instant.parse("2026-07-26T22:00:00Z"));
        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.OUTSIDE_WORKING_HOURS);
    }

    @Test
    @DisplayName("skips duplicate open badge-scoped alerts")
    void skipsDuplicate() {
        badge.setStatus(BadgeStatus.REVOKED);
        when(alertRepository.existsByTypeAndBadgeIdAndStatusIn(any(), any(), any())).thenReturn(true);

        List<Alert> alerts = engine.evaluateBadgeStatusChange(badge, employee);
        assertThat(alerts).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("unknown badge without employee")
    void unknownBadge() {
        event.setEmployee(null);
        List<Alert> alerts = engine.evaluateAccessEvent(event);
        assertThat(alerts).extracting(Alert::getType).contains(AlertType.UNKNOWN_BADGE);
    }
}
