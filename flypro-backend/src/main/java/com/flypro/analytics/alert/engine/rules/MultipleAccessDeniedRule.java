package com.flypro.analytics.alert.engine.rules;

import com.flypro.analytics.accessevent.entity.AccessEvent;
import com.flypro.analytics.accessevent.repository.AccessEventRepository;
import com.flypro.analytics.alert.engine.AlertCandidate;
import com.flypro.analytics.alert.engine.AlertEvaluationContext;
import com.flypro.analytics.alert.engine.AlertRule;
import com.flypro.analytics.alert.engine.AlertTriggerSource;
import com.flypro.analytics.badge.entity.Badge;
import com.flypro.analytics.common.enums.AccessResult;
import com.flypro.analytics.common.enums.AlertSeverity;
import com.flypro.analytics.common.enums.AlertType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Detects three consecutive denied/expired accesses within five minutes for the same badge.
 */
@Component
public class MultipleAccessDeniedRule implements AlertRule {

    private static final int THRESHOLD = 3;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final AccessEventRepository accessEventRepository;

    public MultipleAccessDeniedRule(AccessEventRepository accessEventRepository) {
        this.accessEventRepository = accessEventRepository;
    }

    @Override
    public String name() {
        return "MultipleAccessDeniedRule";
    }

    @Override
    public boolean supports(AlertTriggerSource source) {
        return source == AlertTriggerSource.ACCESS_EVENT;
    }

    @Override
    public List<AlertCandidate> evaluate(AlertEvaluationContext context) {
        AccessEvent event = context.accessEvent();
        Badge badge = context.badge();
        if (event == null || badge == null || badge.getId() == null) {
            return List.of();
        }
        if (event.getResult() != AccessResult.DENIED && event.getResult() != AccessResult.EXPIRED) {
            return List.of();
        }

        Instant since = event.getEventTimestamp().minus(WINDOW);
        long count = accessEventRepository.countByBadgeIdAndResultInAndEventTimestampGreaterThanEqual(
                badge.getId(),
                List.of(AccessResult.DENIED, AccessResult.EXPIRED),
                since
        );

        if (count < THRESHOLD) {
            return List.of();
        }

        return List.of(AlertCandidate.of(
                AlertType.MULTIPLE_ACCESS_DENIED,
                AlertSeverity.CRITICAL,
                "Multiple access denials",
                "Badge " + badge.getBadgeNumber() + " has " + count
                        + " denied/expired accesses within 5 minutes",
                badge, context.employee(), event
        ));
    }
}
