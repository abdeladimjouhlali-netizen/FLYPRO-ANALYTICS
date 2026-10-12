package com.flypro.analytics.alert.engine.rules;

import com.flypro.analytics.alert.engine.AlertCandidate;
import com.flypro.analytics.alert.engine.AlertEvaluationContext;
import com.flypro.analytics.alert.engine.AlertRule;
import com.flypro.analytics.alert.engine.AlertTriggerSource;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.enums.EmployeeStatus;
import com.flypro.analytics.employee.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Alerts for inactive employees and badges without an active assignment.
 */
@Component
public class EmployeeAndUnknownBadgeRule implements AlertRule {

    @Override
    public String name() {
        return "EmployeeAndUnknownBadgeRule";
    }

    @Override
    public boolean supports(AlertTriggerSource source) {
        return source == AlertTriggerSource.ACCESS_EVENT;
    }

    @Override
    public List<AlertCandidate> evaluate(AlertEvaluationContext context) {
        List<AlertCandidate> candidates = new ArrayList<>();
        if (context.accessEvent() == null) {
            return candidates;
        }

        Employee employee = context.employee();
        if (employee == null) {
            candidates.add(AlertCandidate.of(
                    AlertType.UNKNOWN_BADGE,
                    AlertSeverity.HIGH,
                    "Unknown badge usage",
                    "Badge " + (context.badge() != null ? context.badge().getBadgeNumber() : "?")
                            + " has no active employee assignment",
                    context.badge(), null, context.accessEvent()
            ));
            return candidates;
        }

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            candidates.add(AlertCandidate.of(
                    AlertType.INACTIVE_EMPLOYEE,
                    AlertSeverity.HIGH,
                    "Inactive employee access",
                    "Employee " + employee.getEmployeeNumber()
                            + " is " + employee.getStatus() + " but attempted access",
                    context.badge(), employee, context.accessEvent()
            ));
        }

        return candidates;
    }
}
