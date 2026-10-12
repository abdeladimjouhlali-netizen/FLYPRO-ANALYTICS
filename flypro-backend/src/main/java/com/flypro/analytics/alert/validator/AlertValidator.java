package com.flypro.analytics.alert.validator;

import com.flypro.analytics.accessevent.exception.AccessEventNotFoundException;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.alert.dto.AlertRequestDTO;
import com.flypro.analytics.alert.entity.Alert;
import com.flypro.analytics.alert.exception.DuplicateAlertException;
import com.flypro.analytics.alert.exception.InvalidAlertStateException;
import com.flypro.analytics.alert.repository.AlertRepository;
import com.flypro.analytics.badge.exception.BadgeNotFoundException;
import com.flypro.analytics.badge.repository.BadgeRepository;
import com.flypro.analytics.common.enums.AlertStatus;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.employee.exception.EmployeeNotFoundException;
import com.flypro.analytics.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Validation for alert creation and lifecycle transitions.
 */
@Component
public class AlertValidator {

    private static final Set<AlertStatus> OPEN_STATUSES = EnumSet.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final EmployeeRepository employeeRepository;
    private final BadgeRepository badgeRepository;
    private final AccessEventRepository accessEventRepository;
    private final AlertRepository alertRepository;

    public AlertValidator(EmployeeRepository employeeRepository,
                          BadgeRepository badgeRepository,
                          AccessEventRepository accessEventRepository,
                          AlertRepository alertRepository) {
        this.employeeRepository = employeeRepository;
        this.badgeRepository = badgeRepository;
        this.accessEventRepository = accessEventRepository;
        this.alertRepository = alertRepository;
    }

    public void validateCreate(AlertRequestDTO request) {
        if (request.employeeId() != null && !employeeRepository.existsById(request.employeeId())) {
            throw new EmployeeNotFoundException(request.employeeId());
        }
        if (request.badgeId() != null && !badgeRepository.existsById(request.badgeId())) {
            throw new BadgeNotFoundException(request.badgeId());
        }
        if (request.accessEventId() != null && !accessEventRepository.existsById(request.accessEventId())) {
            throw new AccessEventNotFoundException(request.accessEventId());
        }

        if (request.accessEventId() != null
                && alertRepository.existsByTypeAndAccessEventId(request.type(), request.accessEventId())) {
            throw new DuplicateAlertException(request.type());
        }

        if (request.badgeId() != null && isBadgeScoped(request.type())
                && alertRepository.existsByTypeAndBadgeIdAndStatusIn(
                request.type(), request.badgeId(), OPEN_STATUSES)) {
            throw new DuplicateAlertException(request.type());
        }
    }

    public void validateAcknowledge(Alert alert) {
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new InvalidAlertStateException(
                    "Only OPEN alerts can be acknowledged (current: " + alert.getStatus() + ")");
        }
    }

    public void validateResolve(Alert alert) {
        if (alert.getStatus() != AlertStatus.OPEN && alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new InvalidAlertStateException(
                    "Only OPEN or ACKNOWLEDGED alerts can be resolved (current: " + alert.getStatus() + ")");
        }
    }

    public void validateClose(Alert alert) {
        if (alert.getStatus() == AlertStatus.CLOSED) {
            throw new InvalidAlertStateException("Alert is already CLOSED");
        }
    }

    private boolean isBadgeScoped(AlertType type) {
        return type == AlertType.BADGE_EXPIRED
                || type == AlertType.BADGE_REVOKED
                || type == AlertType.BADGE_SUSPENDED
                || type == AlertType.MULTIPLE_ACCESS_DENIED
                || type == AlertType.UNKNOWN_BADGE;
    }
}
