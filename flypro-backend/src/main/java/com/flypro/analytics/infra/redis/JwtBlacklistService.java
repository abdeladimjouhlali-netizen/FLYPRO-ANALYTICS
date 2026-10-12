package com.flypro.analytics.infra.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed JWT blacklist (multi-instance logout / revocation).
 * Only active when {@code flypro.redis.enabled=true}; {@link OptionalJwtBlacklist}
 * falls back to {@link NoOp} otherwise.
 */
@Service
@ConditionalOnProperty(prefix = "flypro.redis", name = "enabled", havingValue = "true")
public class JwtBlacklistService {

    private static final String KEY_PREFIX = "flypro:jwt:blacklist:";

    private final StringRedisTemplate redis;
    private final long accessTokenExpirationMs;

    public JwtBlacklistService(
            StringRedisTemplate redis,
            @Value("${flypro.security.access-token-expiration-ms:900000}") long accessTokenExpirationMs
    ) {
        this.redis = redis;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public void blacklist(String token) {
        redis.opsForValue().set(KEY_PREFIX + token, "1", Duration.ofMillis(accessTokenExpirationMs));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + token));
    }

    /** Used when Redis is disabled — tokens simply expire naturally. */
    public static class NoOp {
        public void blacklist(String token) {
            // no-op
        }

        public boolean isBlacklisted(String token) {
            return false;
        }
    }
}
