# FlyPro Analytics – Backend

Backend Spring Boot de **FlyPro Analytics**, plateforme intelligente de gestion des badges et des accès (PFE).

Ce dépôt contient **uniquement le squelette d'architecture** : Clean Architecture + Domain-Driven Design.  
Aucune logique métier, pas de CRUD, pas d'entités, pas d'authentification.

---

## Présentation

FlyPro Analytics vise à centraliser :

- la gestion des employés et des badges ;
- le contrôle d'accès (zones, points d'accès, profils) ;
- les événements d'accès et les imports de données ;
- le pilotage (dashboard, alertes, audit) ;
- la détection d'anomalies via un service IA (phase ultérieure).

Le backend `flypro-backend` expose l'API REST qui orchestrera ces domaines.

---

## Architecture

Organisation **modulaire par domaine** (approche DDD) :

```
controller  →  service  →  repository / domain
     ↓              ↓
    dto          entity
     ↓
  mapper / validator / specification / exception
```

| Couche | Responsabilité |
|--------|----------------|
| `controller` | Adapters HTTP (ports d'entrée) |
| `service` | Cas d'utilisation |
| `repository` | Persistance (Spring Data JPA) |
| `entity` | Modèle de domaine persisté |
| `dto` | Contrats API |
| `mapper` | MapStruct Entity ↔ DTO |
| `validator` | Règles de validation |
| `specification` | Filtres JPA dynamiques |
| `exception` | Erreurs spécifiques au module |

Packages transverses : `config`, `security`, `common` (constants, enums, exceptions, response, pagination, utils).

---

## Technologies

| Technologie | Usage |
|-------------|--------|
| Java 21 | Langage |
| Spring Boot 3.5 | Framework applicatif |
| Spring Web | API REST |
| Spring Data JPA | Persistance |
| Spring Validation | Validation des entrées |
| Spring Security | Socle sécurité (auth non implémentée) |
| MySQL | Base de données |
| Flyway | Migrations versionnées |
| Lombok | Réduction du boilerplate |
| MapStruct | Mapping objet |
| SpringDoc OpenAPI | Swagger UI |
| Maven | Build |
| Docker | Conteneurisation |

---

## Structure des dossiers

```
flypro-backend/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
├── .env.example
└── src/
    ├── main/
    │   ├── java/com/flypro/analytics/
    │   │   ├── FlyproBackendApplication.java
    │   │   ├── config/          # OpenApi, Security, Cors, Jackson, Application
    │   │   ├── security/
    │   │   ├── common/          # constants, enums, exceptions, response, pagination, utils
    │   │   ├── audit/
    │   │   ├── auth/
    │   │   ├── user/
    │   │   ├── role/
    │   │   ├── employee/
    │   │   ├── department/
    │   │   ├── badge/
    │   │   ├── badgeassignment/
    │   │   ├── accessprofile/
    │   │   ├── zone/
    │   │   ├── accesspoint/
    │   │   ├── accessevent/
    │   │   ├── importbatch/
    │   │   ├── dataquality/
    │   │   ├── alert/
    │   │   ├── anomaly/
    │   │   ├── dashboard/
    │   │   ├── analytics/
    │   │   └── notification/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       ├── db/migration/
    │       ├── static/
    │       └── templates/
    └── test/java/com/flypro/analytics/
```

Chaque module métier contient les packages vides : `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `validator`, `specification`, `exception`.

---

## Prérequis

- JDK **21**
- Maven **3.9+**
- Docker & Docker Compose (optionnel mais recommandé)
- MySQL 8 (si exécution hors Docker)

---

## Variables d'environnement

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | URL JDBC MySQL |
| `DATABASE_USERNAME` | Utilisateur BDD |
| `DATABASE_PASSWORD` | Mot de passe BDD |
| `JWT_SECRET` | Secret réservé à la future auth JWT (non utilisé pour l'instant) |
| `SPRING_PROFILES_ACTIVE` | Profil (`dev` / `prod`) |
| `CORS_ALLOWED_ORIGINS` | Origines CORS autorisées |
| `SERVER_PORT` | Port HTTP (défaut `8080`) |

Copier `.env.example` vers `.env` et adapter les valeurs. **Ne jamais committer de secrets.**

---

## Exécution locale

```bash
# 1. Démarrer MySQL (exemple Docker)
docker compose up -d mysql

# 2. Exporter les variables (PowerShell)
$env:DATABASE_URL="jdbc:mysql://localhost:3306/flypro_analytics?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DATABASE_USERNAME="flypro"
$env:DATABASE_PASSWORD="flypro"
$env:JWT_SECRET="dev-only-change-me-not-for-production"
$env:SPRING_PROFILES_ACTIVE="dev"

# 3. Lancer l'application
mvn spring-boot:run
```

Swagger UI : [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Docker

```bash
# Build + démarrage backend + MySQL
docker compose up -d --build

# Logs backend
docker compose logs -f backend

# Arrêt
docker compose down
```

Services :

| Service | Port |
|---------|------|
| Backend | `8080` |
| MySQL | `3306` |

Réseau Docker : `flypro-network`  
Volume : `flypro_mysql_data`

---

## Compilation

```bash
mvn -DskipTests clean package
```

Le projet doit compiler sans erreur (squelette sans modules métier).

---

## Roadmap de développement

1. **Socle commun** — réponses API, pagination, exceptions globales  
2. **Sécurité** — authentification, JWT, rôles (`auth`, `user`, `role`)  
3. **Organisation** — `department`, `employee`  
4. **Accès** — `zone`, `accesspoint`, `accessprofile`, `badge`, `badgeassignment`  
5. **Événements & imports** — `accessevent`, `importbatch`, `dataquality`  
6. **Pilotage** — `dashboard`, `alert`, `notification`, `audit`  
7. **Intelligence** — `anomaly`, `analytics` + intégration service IA  
8. **Industrialisation** — tests, observabilité, CI/CD  

---

## Conventions de code

- Packages par **domaine métier**, pas par couche technique globale.
- Les controllers restent minces ; la logique vit dans les services.
- **DTO ≠ Entity** ; mapping via MapStruct.
- Migrations SQL **uniquement** via Flyway (`db/migration`).
- Secrets exclusivement via variables d'environnement.
- JavaDoc sur les types publics de configuration et points d'entrée.
- Pas de logique métier dans `config` ni `common`.

---

## État actuel

| Élément | Statut |
|---------|--------|
| Structure modulaire | Prêt |
| Configurations minimales | Prêt |
| Swagger UI | Prêt (`/swagger-ui.html`) |
| Docker Compose | Prêt |
| Entités / CRUD / Auth JWT | Absents (volontaire) |

---

## Licence

Projet académique **FlyPro Analytics** (PFE) — usage éducatif.
