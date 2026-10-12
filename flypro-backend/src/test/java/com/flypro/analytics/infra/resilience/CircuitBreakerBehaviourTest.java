package com.flypro.analytics.infra.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Circuit breaker open/close behaviour (unit, no Spring context).
 */
class CircuitBreakerBehaviourTest {

    @Test
    @DisplayName("opens after failure threshold then rejects calls")
    void opensOnFailures() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        CircuitBreaker cb = CircuitBreakerRegistry.of(config).circuitBreaker("aiService");

        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 4; i++) {
            try {
                cb.executeRunnable(() -> {
                    calls.incrementAndGet();
                    throw new RuntimeException("boom");
                });
            } catch (RuntimeException ignored) {
                // expected
            }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(calls.get()).isEqualTo(4);

        boolean rejected = false;
        try {
            cb.executeRunnable(calls::incrementAndGet);
        } catch (Exception e) {
            rejected = true;
        }
        assertThat(rejected).isTrue();
        assertThat(calls.get()).isEqualTo(4);
    }
}
