package com.flypro.analytics.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration CORS de l'API.
 *
 * <p>Utilise {@code allowedOriginPatterns} pour supporter les ports Vite variables
 * (5173, 5174, 5175…) en développement local.</p>
 */
@Configuration
public class CorsConfig {

    @Value("${flypro.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*,http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Source de configuration CORS appliquée à l'ensemble de l'API.
     *
     * @return source CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(splitOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> splitOrigins(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
