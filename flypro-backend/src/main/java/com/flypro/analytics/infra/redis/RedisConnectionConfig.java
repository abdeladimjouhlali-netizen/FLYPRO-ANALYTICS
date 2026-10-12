package com.flypro.analytics.infra.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Manual Redis connection (auto-config excluded by default to allow boot without Redis).
 */
@Configuration
@ConditionalOnProperty(prefix = "flypro.redis", name = "enabled", havingValue = "true")
public class RedisConnectionConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.timeout:2s}") Duration timeout
    ) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            standalone.setPassword(password);
        }
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .build();
        return new LettuceConnectionFactory(standalone, client);
    }
}
