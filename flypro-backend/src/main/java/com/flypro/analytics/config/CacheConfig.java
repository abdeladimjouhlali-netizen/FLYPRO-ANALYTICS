package com.flypro.analytics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Local Caffeine cache (default). Replaced by Redis when {@code flypro.redis.enabled=true}.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "flypro.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class CacheConfig {

    public static final String REF_DATA = "refData";
    public static final String DASHBOARD = "dashboard";
    public static final String ANALYTICS = "analytics";
    public static final String SESSION = "session";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(REF_DATA, DASHBOARD, ANALYTICS, SESSION);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
