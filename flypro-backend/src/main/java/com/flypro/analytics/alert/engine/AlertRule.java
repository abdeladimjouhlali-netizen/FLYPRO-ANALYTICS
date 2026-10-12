package com.flypro.analytics.alert.engine;

import java.util.List;

/**
 * Pluggable alert rule. Future AI detectors can implement this interface
 * and be registered as Spring beans without changing the engine.
 */
public interface AlertRule {

    /**
     * Unique rule name for diagnostics.
     */
    String name();

    /**
     * Whether this rule applies to the given trigger source.
     */
    boolean supports(AlertTriggerSource source);

    /**
     * Evaluate the context and return zero or more alert candidates.
     */
    List<AlertCandidate> evaluate(AlertEvaluationContext context);
}
