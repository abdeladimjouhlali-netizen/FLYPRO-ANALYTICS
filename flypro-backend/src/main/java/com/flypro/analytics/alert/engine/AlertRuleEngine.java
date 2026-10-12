package com.flypro.analytics.alert.engine;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.importbatch.entity.ImportBatch;

import java.util.List;

/**
 * Rule engine that evaluates business (and future AI) rules and persists alerts.
 */
public interface AlertRuleEngine {

    /**
     * Evaluate and persist alerts related to a persisted access event.
     */
    List<Alert> evaluateAccessEvent(AccessEvent event);

    /**
     * Evaluate and persist alerts related to a badge status change.
     */
    List<Alert> evaluateBadgeStatusChange(Badge badge, Employee employee);

    /**
     * Evaluate and persist alerts related to import data quality.
     */
    List<Alert> evaluateImportQuality(ImportBatch batch);
}
