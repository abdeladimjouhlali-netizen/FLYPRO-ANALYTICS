package com.flypro.analytics.infra.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("blacklist stores token with TTL prefix")
    void blacklistStoresKey() {
        when(redis.opsForValue()).thenReturn(valueOps);
        JwtBlacklistService service = new JwtBlacklistService(redis, 120_000);

        service.blacklist("tok-abc");

        verify(valueOps).set(eq("flypro:jwt:blacklist:tok-abc"), eq("1"), eq(Duration.ofMillis(120_000)));
    }

    @Test
    @DisplayName("isBlacklisted reads Redis key presence")
    void isBlacklisted() {
        when(redis.hasKey("flypro:jwt:blacklist:tok-abc")).thenReturn(true);
        JwtBlacklistService service = new JwtBlacklistService(redis, 60_000);

        assertThat(service.isBlacklisted("tok-abc")).isTrue();
        assertThat(service.isBlacklisted("")).isFalse();
        assertThat(service.isBlacklisted(null)).isFalse();
    }

    @Test
    @DisplayName("NoOp never blacklists")
    void noOp() {
        JwtBlacklistService.NoOp noOp = new JwtBlacklistService.NoOp();
        noOp.blacklist("x");
        assertThat(noOp.isBlacklisted("x")).isFalse();
    }
}
