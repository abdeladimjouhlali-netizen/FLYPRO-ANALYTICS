package com.flypro.analytics.alert.engine.rules;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.alert.engine.AlertCandidate;
import com.flypro.analytics.alert.engine.AlertEvaluationContext;
import com.flypro.analytics.alert.engine.AlertRule;
import com.flypro.analytics.alert.engine.AlertTriggerSource;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import com.flypro.analytics.common.enums.BadgeStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Raises alerts for expired / revoked / suspended badges and generic access denials.
 */
@Component
public class BadgeAndAccessDenialRule implements AlertRule {

    private static final Set<AlertTriggerSource> SOURCES = EnumSet.of(
            AlertTriggerSource.ACCESS_EVENT, AlertTriggerSource.BADGE_STATUS
    );

    @Override
    public String name() {
        return "BadgeAndAccessDenialRule";
    }

    @Override
    public boolean supports(AlertTriggerSource source) {
        return SOURCES.contains(source);
    }

    @Override
    public List<AlertCandidate> evaluate(AlertEvaluationContext context) {
        List<AlertCandidate> candidates = new ArrayList<>();
        Badge badge = context.badge();
        if (badge == null) {
            return candidates;
        }

        AccessEvent event = context.accessEvent();

        if (badge.getStatus() == BadgeStatus.EXPIRED
                || (event != null && event.getResult() == AccessResult.EXPIRED)) {
            candidates.add(AlertCandidate.of(
                    AlertType.BADGE_EXPIRED,
                    AlertSeverity.HIGH,
                    "Badge expired",
                    "Badge " + badge.getBadgeNumber() + " is expired",
                    badge, context.employee(), event
            ));
        }

        if (badge.getStatus() == BadgeStatus.REVOKED) {
            candidates.add(AlertCandidate.of(
                    AlertType.BADGE_REVOKED,
                    AlertSeverity.CRITICAL,
                    "Badge revoked",
                    "Badge " + badge.getBadgeNumber() + " is revoked",
                    badge, context.employee(), event
            ));
        }

        if (badge.getStatus() == BadgeStatus.SUSPENDED) {
            candidates.add(AlertCandidate.of(
                    AlertType.BADGE_SUSPENDED,
                    AlertSeverity.HIGH,
                    "Badge suspended",
                    "Badge " + badge.getBadgeNumber() + " is suspended",
                    badge, context.employee(), event
            ));
        }

        if (event != null && event.getResult() == AccessResult.DENIED) {
            candidates.add(AlertCandidate.of(
                    AlertType.ACCESS_DENIED,
                    AlertSeverity.MEDIUM,
                    "Access denied",
                    "Access denied for badge " + badge.getBadgeNumber()
                            + (event.getRefusalReason() != null ? ": " + event.getRefusalReason() : ""),
                    badge, context.employee(), event
            ));
        }

        return candidates;
    }
}
