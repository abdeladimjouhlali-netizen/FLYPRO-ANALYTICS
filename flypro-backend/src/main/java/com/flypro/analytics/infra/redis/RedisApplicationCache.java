package com.flypro.analytics.infra.redis;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Generic Redis application cache helpers (dashboard / analytics keys).
 */
@Service
public class RedisApplicationCache {

    private final StringRedisTemplate redis;

    public RedisApplicationCache(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redis = redisProvider.getIfAvailable();
    }

    public boolean available() {
        return redis != null;
    }

    public void put(String key, String value, Duration ttl) {
        if (redis == null) {
            return;
        }
        redis.opsForValue().set(key, value, ttl);
    }

    public Optional<String> get(String key) {
        if (redis == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    public void evict(String key) {
        if (redis != null) {
            redis.delete(key);
        }
    }
}
