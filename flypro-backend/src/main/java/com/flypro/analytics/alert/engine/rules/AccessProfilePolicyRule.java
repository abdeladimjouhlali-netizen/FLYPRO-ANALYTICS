package com.flypro.analytics.alert.engine.rules;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessprofile.entity.AccessProfile;
import com.flypro.analytics.accessprofile.repository.AccessProfileRepository;
import com.flypro.analytics.alert.engine.AlertCandidate;
import com.flypro.analytics.alert.engine.AlertEvaluationContext;
import com.flypro.analytics.alert.engine.AlertRule;
import com.flypro.analytics.alert.engine.AlertTriggerSource;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.zone.entity.Zone;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Working-hours and authorized-zone policy rules based on the employee access profile.
 */
@Component
public class AccessProfilePolicyRule implements AlertRule {

    private final AccessProfileRepository accessProfileRepository;

    public AccessProfilePolicyRule(AccessProfileRepository accessProfileRepository) {
        this.accessProfileRepository = accessProfileRepository;
    }

    @Override
    public String name() {
        return "AccessProfilePolicyRule";
    }

    @Override
    public boolean supports(AlertTriggerSource source) {
        return source == AlertTriggerSource.ACCESS_EVENT;
    }

    @Override
    public List<AlertCandidate> evaluate(AlertEvaluationContext context) {
        List<AlertCandidate> candidates = new ArrayList<>();
        AccessEvent event = context.accessEvent();
        Employee employee = context.employee();
        Zone zone = context.zone();
        if (event == null || employee == null || employee.getAccessProfile() == null) {
            return candidates;
        }

        AccessProfile profile = accessProfileRepository.findByIdWithZones(employee.getAccessProfile().getId())
                .orElse(employee.getAccessProfile());

        LocalTime eventTime = event.getEventTimestamp().atZone(ZoneOffset.UTC).toLocalTime();
        if (isOutsideWorkingHours(eventTime, profile.getStartTime(), profile.getEndTime())) {
            candidates.add(AlertCandidate.of(
                    AlertType.OUTSIDE_WORKING_HOURS,
                    AlertSeverity.MEDIUM,
                    "Access outside working hours",
                    "Employee " + employee.getEmployeeNumber() + " accessed at " + eventTime
                            + " outside profile window " + profile.getStartTime() + "-" + profile.getEndTime(),
                    context.badge(), employee, event
            ));
        }

        if (zone != null && profile.getZones() != null && !profile.getZones().isEmpty()) {
            boolean authorized = profile.getZones().stream()
                    .anyMatch(z -> z.getId() != null && z.getId().equals(zone.getId()));
            if (!authorized) {
                candidates.add(AlertCandidate.of(
                        AlertType.UNAUTHORIZED_ZONE,
                        AlertSeverity.HIGH,
                        "Unauthorized zone access",
                        "Employee " + employee.getEmployeeNumber()
                                + " attempted access to unauthorized zone " + zone.getName(),
                        context.badge(), employee, event
                ));
            }
        }

        return candidates;
    }

    private boolean isOutsideWorkingHours(LocalTime eventTime, LocalTime start, LocalTime end) {
        if (start == null || end == null || start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return eventTime.isBefore(start) || !eventTime.isBefore(end);
        }
        return !eventTime.isBefore(end) && eventTime.isBefore(start);
    }
}
