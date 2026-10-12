package com.flypro.analytics.infra.redis;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Facade so security filters work whether Redis is on or off.
 */
@Component
public class OptionalJwtBlacklist {

    private final JwtBlacklistService redisService;
    private final JwtBlacklistService.NoOp noOp = new JwtBlacklistService.NoOp();

    public OptionalJwtBlacklist(ObjectProvider<JwtBlacklistService> provider) {
        this.redisService = provider.getIfAvailable();
    }

    public void blacklist(String token) {
        if (redisService != null) {
            redisService.blacklist(token);
        } else {
            noOp.blacklist(token);
        }
    }

    public boolean isBlacklisted(String token) {
        return redisService != null ? redisService.isBlacklisted(token) : noOp.isBlacklisted(token);
    }
}
