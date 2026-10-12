package com.flypro.analytics.security.bruteforce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login attempt tracker to mitigate credential stuffing / brute force.
 */
@Service
public class LoginAttemptService {

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMillis;

    public LoginAttemptService(
            @Value("${flypro.security.brute-force.max-attempts:5}") int maxAttempts,
            @Value("${flypro.security.brute-force.lock-seconds:300}") long lockSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.lockMillis = lockSeconds * 1000L;
    }

    public void assertNotLocked(String username) {
        Attempt attempt = attempts.get(normalize(username));
        if (attempt == null) {
            return;
        }
        if (attempt.lockedUntil != null && Instant.now().isBefore(attempt.lockedUntil)) {
            throw new AccountTemporarilyLockedException(
                    "Account temporarily locked due to too many failed login attempts"
            );
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(normalize(username));
    }

    public void loginFailed(String username) {
        String key = normalize(username);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        attempt.failures++;
        if (attempt.failures >= maxAttempts) {
            attempt.lockedUntil = Instant.now().plusMillis(lockMillis);
            attempt.failures = 0;
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Attempt {
        private int failures;
        private Instant lockedUntil;
    }
}
