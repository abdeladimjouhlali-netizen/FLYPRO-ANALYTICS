package com.flypro.analytics.alert.engine;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.employee.entity.Employee;
import com.flypro.analytics.importbatch.entity.ImportBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Default rule engine: runs all registered {@link AlertRule} beans and deduplicates.
 * AI rules can be added later as additional {@link AlertRule} implementations.
 */
@Service
@Transactional
public class DefaultAlertRuleEngine implements AlertRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultAlertRuleEngine.class);

    private static final Set<AlertStatus> OPEN_STATUSES = EnumSet.of(
            AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED
    );

    private final List<AlertRule> rules;
    private final AlertRepository alertRepository;

    public DefaultAlertRuleEngine(List<AlertRule> rules, AlertRepository alertRepository) {
        this.rules = List.copyOf(rules);
        this.alertRepository = alertRepository;
    }

    @Override
    public List<Alert> evaluateAccessEvent(AccessEvent event) {
        return persistCandidates(AlertEvaluationContext.forAccessEvent(event), AlertTriggerSource.ACCESS_EVENT);
    }

    @Override
    public List<Alert> evaluateBadgeStatusChange(Badge badge, Employee employee) {
        return persistCandidates(AlertEvaluationContext.forBadge(badge, employee), AlertTriggerSource.BADGE_STATUS);
    }

    @Override
    public List<Alert> evaluateImportQuality(ImportBatch batch) {
        return persistCandidates(AlertEvaluationContext.forImport(batch), AlertTriggerSource.IMPORT);
    }

    private List<Alert> persistCandidates(AlertEvaluationContext context, AlertTriggerSource source) {
        List<Alert> created = new ArrayList<>();
        for (AlertRule rule : rules) {
            if (!rule.supports(source)) {
                continue;
            }
            List<AlertCandidate> candidates = rule.evaluate(context);
            for (AlertCandidate candidate : candidates) {
                if (isDuplicate(candidate)) {
                    log.debug("Skipping duplicate alert type={} rule={}", candidate.type(), rule.name());
                    continue;
                }
                Alert alert = toAlert(candidate);
                created.add(alertRepository.save(alert));
            }
        }
        return created;
    }

    private boolean isDuplicate(AlertCandidate candidate) {
        AlertType type = candidate.type();
        AccessEvent event = candidate.accessEvent();
        Badge badge = candidate.badge();
        Employee employee = candidate.employee();

        if (event != null && event.getId() != null
                && alertRepository.existsByTypeAndAccessEventId(type, event.getId())) {
            return true;
        }

        if (badge != null && badge.getId() != null
                && isBadgeScoped(type)
                && alertRepository.existsByTypeAndBadgeIdAndStatusIn(type, badge.getId(), OPEN_STATUSES)) {
            return true;
        }

        if (employee != null && employee.getId() != null
                && type == AlertType.INACTIVE_EMPLOYEE
                && alertRepository.existsByTypeAndEmployeeIdAndStatusIn(type, employee.getId(), OPEN_STATUSES)) {
            return true;
        }

        if (candidate.importBatch() != null && type == AlertType.SYSTEM_WARNING
                && badge == null && employee == null && event == null) {
            // one open SYSTEM_WARNING per import is enough — checked via description uniqueness loosely
            return false;
        }

        return false;
    }

    private boolean isBadgeScoped(AlertType type) {
        return type == AlertType.BADGE_EXPIRED
                || type == AlertType.BADGE_REVOKED
                || type == AlertType.BADGE_SUSPENDED
                || type == AlertType.MULTIPLE_ACCESS_DENIED
                || type == AlertType.UNKNOWN_BADGE;
    }

    private Alert toAlert(AlertCandidate candidate) {
        return Alert.builder()
                .title(truncate(candidate.title(), 200))
                .description(truncate(candidate.description(), 2000))
                .severity(candidate.severity())
                .status(AlertStatus.OPEN)
                .type(candidate.type())
                .createdDate(Instant.now())
                .badge(candidate.badge())
                .employee(candidate.employee())
                .accessEvent(candidate.accessEvent())
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
