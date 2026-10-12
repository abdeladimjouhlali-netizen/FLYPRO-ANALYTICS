package com.flypro.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration CORS.
 *
 * <p>Rôle futur : autoriser les origines du frontend Vite (dev) et des
 * déploiements production. Aucune règle CORS active pour l'instant.</p>
 */
@Configuration
public class CorsConfig {
    // TODO (phase ultérieure) : CorsConfigurationSource / WebMvcConfigurer
}
