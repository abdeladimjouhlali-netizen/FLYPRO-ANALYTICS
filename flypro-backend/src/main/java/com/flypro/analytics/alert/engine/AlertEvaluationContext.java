package com.flypro.analytics.alert.engine;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accesspoint.entity.AccessPoint;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import com.flypro.analytics.zone.entity.Zone;

/**
 * Evaluation context shared by all alert rules.
 * Designed so AI rules can consume the same context without API changes.
 */
public record AlertEvaluationContext(
        AccessEvent accessEvent,
        Badge badge,
        Employee employee,
        Zone zone,
        AccessPoint accessPoint,
        ImportBatch importBatch,
        AlertTriggerSource source
) {
    public static AlertEvaluationContext forAccessEvent(AccessEvent event) {
        return new AlertEvaluationContext(
                event,
                event.getBadge(),
                event.getEmployee(),
                event.getZone(),
                event.getAccessPoint(),
                null,
                AlertTriggerSource.ACCESS_EVENT
        );
    }

    public static AlertEvaluationContext forBadge(Badge badge, Employee employee) {
        return new AlertEvaluationContext(
                null, badge, employee, null, null, null, AlertTriggerSource.BADGE_STATUS
        );
    }

    public static AlertEvaluationContext forImport(ImportBatch batch) {
        return new AlertEvaluationContext(
                null, null, null, null, null, batch, AlertTriggerSource.IMPORT
        );
    }
}
