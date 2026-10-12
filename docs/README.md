# FlyPro Analytics – Documentation Technique (PFE)

**Version :** 1.0.0  
**Phase :** 6 – Step 20 (HA Enterprise)  
**Audience :** jury PFE, mainteneurs, développeurs, livraison client  

---

## Vue d’ensemble

**FlyPro Analytics** est une plateforme de gestion intelligente des badges et accès employés, avec tableaux de bord opérationnels, alertes et détection d’anomalies par Isolation Forest.

| Composant | Technologie | Port (Compose) |
|-----------|-------------|----------------|
| Frontend SPA + gateway | React 19, Vite, Nginx | 80 |
| Backend API | Spring Boot 3.5, Java 21 | 8080 |
| Service IA | FastAPI, Scikit-Learn | 8000 |
| Base de données | **MySQL 8.4** (Flyway) | 3306 |

> **Note infrastructure :** le brief mentionne parfois PostgreSQL ; l’implémentation livrée utilise **MySQL** (dialecte Hibernate + migrations Flyway). Documentez MySQL pour toute installation/déploiement.

---

## Sommaire des documents

| Document | Contenu |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture globale, microservices, flux |
| [INSTALLATION.md](INSTALLATION.md) | Prérequis, installation locale et Docker |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Déploiement, Nginx, variables d’environnement |
| [DATABASE.md](DATABASE.md) | Entités, relations, index, règles métier |
| [API.md](API.md) | Catalogue des endpoints REST |
| [SECURITY.md](SECURITY.md) | JWT, RBAC, headers, rate limiting |
| [AI.md](AI.md) | Isolation Forest, training, prediction |
| [USER_GUIDE.md](USER_GUIDE.md) | Manuel utilisateur métier |
| [MAINTENANCE.md](MAINTENANCE.md) | Structure projet, extension, standards |
| [PRESENTATION.md](PRESENTATION.md) | Support de présentation PFE |
| [QUALITY_REPORT.md](QUALITY_REPORT.md) | Rapport qualité / production (Step 16) |
| [KUBERNETES.md](KUBERNETES.md) | Déploiement Kubernetes (Step 18) |
| [MONITORING.md](MONITORING.md) | Prometheus / Grafana / Actuator |
| [LOGGING.md](LOGGING.md) | Logging centralisé (Fluent Bit / OpenSearch) |
| [SCALING.md](SCALING.md) | HPA, resources, rolling updates |
| [BACKUP.md](BACKUP.md) | Sauvegardes MySQL |
| [RECOVERY.md](RECOVERY.md) | Disaster recovery |
| [TRACING.md](TRACING.md) | OpenTelemetry / Jaeger |
| [REPORTING.md](REPORTING.md) | Reports, exports & notifications (Step 19) |
| [HIGH_AVAILABILITY.md](HIGH_AVAILABILITY.md) | Architecture HA (Step 20) |
| [REDIS.md](REDIS.md) | Cache Redis / JWT blacklist |
| [RABBITMQ.md](RABBITMQ.md) | Broker async + DLQ |
| [RESILIENCE4J.md](RESILIENCE4J.md) | Circuit breaker / retry / bulkhead |
| [API_GATEWAY.md](API_GATEWAY.md) | Nginx gateway & load balancing |
| [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) | DR enterprise + rotation secrets |

---

## Diagrammes UML (PlantUML)

Dossier : [`UML/`](UML/)

| Fichier | Diagramme |
|---------|-----------|
| `usecase.puml` | Cas d’utilisation |
| `class.puml` | Classes domaine |
| `sequence.puml` | Séquences métier |
| `component.puml` | Composants logiciels |
| `deployment.puml` | Déploiement Docker |
| `package.puml` | Packages backend / frontend / AI |
| `erd.puml` | Modèle entité-relation |
| `activity.puml` | Activité (import) |
| `activity-alerts.puml` | Activité (alertes) |
| `auth-flow.puml` | Flux d’authentification JWT |
| `ai-prediction.puml` | Flux de prédiction IA |
| `ha-architecture.puml` | Architecture HA (Step 20) |
| `ha-async-sequence.puml` | Séquence jobs RabbitMQ |
| `ha-resilience-sequence.puml` | Circuit breaker + fallback IA |

### Rendu PlantUML

```bash
# Exemple avec plantuml.jar
java -jar plantuml.jar docs/UML/*.puml -o ../diagrams
```

Ou via extension VS Code / Cursor / IntelliJ PlantUML.

---

## Accès rapides

| Ressource | URL |
|-----------|-----|
| Application (Compose) | http://localhost/ |
| Swagger backend (dev) | http://localhost:8080/swagger-ui.html |
| Docs AI | http://localhost:8000/docs |
| Health backend | http://localhost:8080/actuator/health |
| Health AI | http://localhost:8000/api/v1/ai/health |

**Compte seed :** `admin` / `Admin@123` (`ROLE_SUPER_ADMIN`)

---

## Démarrage express

```bash
cp .env.example .env
# Éditer JWT_SECRET et mots de passe MySQL
docker compose up --build -d
```

Guides détaillés : [INSTALLATION.md](INSTALLATION.md) · [DEPLOYMENT.md](DEPLOYMENT.md)

---

## Conformité Step 17

Cette documentation couvre :

- Architecture système (globale, FE, BE, AI, DB, Docker)
- UML PlantUML (use case, class, sequence, component, deployment, package, ERD, activity, auth, AI)
- Base de données (toutes les entités)
- API REST (auth → AI)
- Sécurité, IA, déploiement, installation, maintenance
- Manuel utilisateur et support de présentation PFE

**Aucun code applicatif n’a été modifié** dans le cadre de cette étape : documentation uniquement.
