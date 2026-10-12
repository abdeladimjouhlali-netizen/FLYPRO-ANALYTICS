package com.flypro.analytics.infra.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevent double registration of security filters (SecurityFilterChain only).
 */
@Configuration
public class SecurityFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtBlacklistFilter> jwtBlacklistRegistration(JwtBlacklistFilter filter) {
        FilterRegistrationBean<JwtBlacklistFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
