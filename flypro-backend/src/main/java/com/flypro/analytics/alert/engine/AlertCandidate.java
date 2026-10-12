package com.flypro.analytics.alert.engine;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.importbatch.entity.ImportBatch;

/**
 * Candidate alert produced by a business (or future AI) rule before persistence.
 */
public record AlertCandidate(
        AlertType type,
        AlertSeverity severity,
        String title,
        String description,
        Badge badge,
        Employee employee,
        AccessEvent accessEvent,
        ImportBatch importBatch
) {
    public static AlertCandidate of(AlertType type,
                                    AlertSeverity severity,
                                    String title,
                                    String description,
                                    Badge badge,
                                    Employee employee,
                                    AccessEvent accessEvent) {
        return new AlertCandidate(type, severity, title, description, badge, employee, accessEvent, null);
    }
}
