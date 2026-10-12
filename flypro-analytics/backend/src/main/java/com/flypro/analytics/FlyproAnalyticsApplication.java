package com.flypro.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée Spring Boot de FlyPro Analytics.
 *
 * <p>Rôle futur : démarrer le contexte applicatif du monolithe modulaire
 * (badge, employés, accès, alertes, analytics, etc.). Aucune logique métier
 * n'est implémentée à ce stade — squelette d'architecture uniquement.</p>
 */
@SpringBootApplication
public class FlyproAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlyproAnalyticsApplication.class, args);
    }
}
