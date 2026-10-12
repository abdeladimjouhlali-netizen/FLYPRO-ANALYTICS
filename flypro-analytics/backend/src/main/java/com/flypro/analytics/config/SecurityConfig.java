package com.flypro.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring Security.
 *
 * <p>Rôle futur : chaînes de filtres, authentification JWT/session,
 * autorisation par rôles, CORS lié à {@link CorsConfig}, et protection
 * des endpoints. Aucune règle de sécurité n'est implémentée ici.</p>
 */
@Configuration
public class SecurityConfig {
    // TODO (phase ultérieure) : SecurityFilterChain, AuthenticationManager, PasswordEncoder
}
