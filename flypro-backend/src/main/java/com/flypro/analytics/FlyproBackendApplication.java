package com.flypro.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application FlyPro Analytics (backend).
 *
 * <p>Ce démarrage initialise le contexte Spring Boot du monolithe modulaire.
 * Aucune logique métier n'est exécutée à ce stade : le projet constitue
 * uniquement le squelette Clean Architecture / DDD prêt pour l'implémentation
 * progressive des modules (employés, badges, accès, alertes, analytics, etc.).</p>
 */
@SpringBootApplication
public class FlyproBackendApplication {

    /**
     * Lance l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(FlyproBackendApplication.class, args);
    }
}
