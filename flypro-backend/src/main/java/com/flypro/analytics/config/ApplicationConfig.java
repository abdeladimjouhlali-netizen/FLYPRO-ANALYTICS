package com.flypro.analytics.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration applicative transversale.
 *
 * <p>Responsabilité future : centraliser les beans techniques partagés
 * (horloges, convertisseurs, propriétés typées) et activer le scan des
 * {@code @ConfigurationProperties}. Aucune logique métier ici.</p>
 */
@Configuration
@ConfigurationPropertiesScan(basePackages = "com.flypro.analytics")
public class ApplicationConfig {
}
