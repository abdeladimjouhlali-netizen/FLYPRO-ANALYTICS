package com.flypro.analytics.config;

import com.flypro.analytics.infra.security.ApiKeyAuthFilter;
import com.flypro.analytics.infra.security.JwtBlacklistFilter;
import com.flypro.analytics.security.dev.DevAuthBypassFilter;
import com.flypro.analytics.security.handler.SecurityExceptionHandler;
import com.flypro.analytics.security.jwt.JwtAuthenticationFilter;
import com.flypro.analytics.security.jwt.JwtProperties;
import com.flypro.analytics.security.ratelimit.RateLimitFilter;
import com.flypro.analytics.security.userdetails.CustomUserDetailsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Arrays;

/**
 * Configuration Spring Security (JWT, stateless, CORS, BCrypt, security headers).
 * Set {@code flypro.security.auth-enabled=false} in local/dev to temporarily bypass JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JwtBlacklistFilter jwtBlacklistFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final ObjectProvider<DevAuthBypassFilter> devAuthBypassFilter;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final Environment environment;

    @Value("${flypro.security.swagger-enabled:true}")
    private boolean swaggerEnabled;

    @Value("${flypro.security.auth-enabled:true}")
    private boolean authEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          JwtBlacklistFilter jwtBlacklistFilter,
                          ApiKeyAuthFilter apiKeyAuthFilter,
                          ObjectProvider<DevAuthBypassFilter> devAuthBypassFilter,
                          CustomUserDetailsService userDetailsService,
                          SecurityExceptionHandler securityExceptionHandler,
                          Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.jwtBlacklistFilter = jwtBlacklistFilter;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.devAuthBypassFilter = devAuthBypassFilter;
        this.userDetailsService = userDetailsService;
        this.securityExceptionHandler = securityExceptionHandler;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean allowSwagger = swaggerEnabled && !prod;

        // Never allow auth bypass on prod
        boolean bypassAuth = !authEnabled && !prod;

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .permissionsPolicy(permissions ->
                                permissions.policy("geolocation=(), microphone=(), camera=()"))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler))
                .authorizeHttpRequests(auth -> {
                    if (bypassAuth) {
                        auth.anyRequest().permitAll();
                        return;
                    }
                    auth.requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info"
                    ).permitAll();
                    if (allowSwagger) {
                        auth.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll();
                    }
                    auth.requestMatchers("/actuator/prometheus").hasAnyRole("SUPER_ADMIN", "ADMIN");
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider());

        if (bypassAuth) {
            DevAuthBypassFilter bypass = devAuthBypassFilter.getIfAvailable();
            if (bypass != null) {
                http.addFilterBefore(bypass, UsernamePasswordAuthenticationFilter.class);
            }
        } else {
            http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtBlacklistFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
