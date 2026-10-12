package com.flypro.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration Jackson (sérialisation JSON).
 *
 * <p>Rôle futur : ObjectMapper personnalisé (dates ISO-8601, enums,
 * ignore unknown properties, modules JavaTime). Squelette uniquement.</p>
 */
@Configuration
public class JacksonConfig {
    // TODO (phase ultérieure) : beans ObjectMapper / Jackson2ObjectMapperBuilderCustomizer
}
