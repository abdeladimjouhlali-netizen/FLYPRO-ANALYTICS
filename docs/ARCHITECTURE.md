# Architecture système – FlyPro Analytics

**Version :** 1.0.0  
**Audience :** architectes, développeurs, jury PFE  

---

## 1. Architecture globale

FlyPro Analytics est une plateforme **multi-services** orchestrée par Docker Compose. Elle sépare clairement la présentation, le métier, l’intelligence artificielle et la persistance.

```text
┌────────────┐     ┌──────────────────────┐     ┌────────────────────┐
│ Navigateur │────▶│ Nginx (frontend:80)  │────▶│ Spring Boot :8080  │
└────────────┘     │  SPA React statique  │     │ JWT · JPA · Flyway │
                   │  /api  → backend     │     └─────────┬──────────┘
                   │  /ai-api → AI        │               │ JDBC
                   └──────────┬───────────┘               ▼
                              │                    ┌────────────┐
                              └───────────────────▶│ MySQL 8.4  │
                                                   └────────────┘
                              ┌────────────────────┐
                              │ FastAPI AI :8000   │
                              │ Isolation Forest   │
                              └────────────────────┘
```

### Principes

| Principe | Application |
|----------|-------------|
| Séparation des responsabilités | UI / API / IA / DB isolés |
| Gateway unique | Nginx expose le port 80 |
| Stateless API | JWT Bearer, pas de session serveur |
| Migrations versionnées | Flyway V1–V10 |
| Observabilité | Actuator, healthchecks, Prometheus (préparé) |

> **Note :** le brief historique cite parfois PostgreSQL. L’implémentation livrée et le Compose racine utilisent **MySQL 8.4**.

---

## 2. Architecture « microservice »

Trois services applicatifs indépendants + une base de données :

| Service | Technologie | Responsabilité | État |
|---------|-------------|----------------|------|
| `flypro-frontend` | React 19 + Nginx | SPA + reverse proxy | Stateless |
| `flypro-backend` | Spring Boot 3.5 / Java 21 | Métier, sécurité, persistance | Stateful (DB) |
| `flypro-ai` | FastAPI / Python 3.12 | Anomalies Isolation Forest | Stateful (fichiers modèles) |
| `flypro-mysql` | MySQL 8.4 | Persistance relationnelle | Volumes |

### Communication entre services

| Flux | Protocole | Authentification |
|------|-----------|------------------|
| Browser → Nginx → Backend | HTTP `/api/v1/*` | Bearer JWT |
| Browser → Nginx → AI | HTTP `/ai-api` → FastAPI | Ouvert (réseau interne recommandé) |
| Backend → AI | HTTP `AI_SERVICE_URL` | URL interne Docker |
| Backend → MySQL | JDBC | User/password |

Réseau Docker : `flypro-net` (bridge).

---

## 3. Architecture Frontend

**Dossier :** `flypro-frontend/`

```text
src/
├── api/              # Axios clients (backend + AI), tokenStore, services
├── components/       # UI (Button, DataTable, Charts…) + shared
├── constants/        # Rôles, navigation
├── contexts/         # AuthContext, ThemeContext
├── features/         # Pages par domaine métier
├── layouts/          # AuthLayout, DashboardLayout
├── routes/           # AppRouter, ProtectedRoute
├── types/            # DTOs TypeScript
└── utils/
```

### Choix techniques

- **React 19** + **TypeScript** + **Vite 8**
- **TanStack Query** pour le cache serveur (pas de Redux)
- **React Router 7** avec lazy-loading + `Suspense`
- **react-hook-form** + **Zod** pour les formulaires
- **Tailwind CSS 4** + **Recharts**
- Proxy Vite en dev : `/api` → `:8080`, `/ai-api` → `:8000`

### Sécurité côté UI

- `ProtectedRoute` : redirection login si non authentifié
- Filtrage sidebar par rôle (`NAV_ITEMS` + `canAccess`)
- Intercepteur Axios : refresh automatique sur 401

---

## 4. Architecture Backend

**Dossier :** `flypro-backend/`  
**Package racine :** `com.flypro.analytics`

Organisation **modulaire par domaine** (style hexagonal léger) :

```text
auth · user · department · employee · badge · badgeassignment
zone · accessprofile · accesspoint · accessevent
importbatch · alert · dashboard
anomaly · audit · dataquality   (entités / support)
common · config · security
```

### Couches par module

1. **controller** — REST, validation, `@PreAuthorize`
2. **service** — règles métier
3. **repository** — Spring Data JPA + Specifications
4. **entity / dto / mapper** — MapStruct
5. **validator / exception** — contraintes locales

### Cross-cutting

- Spring Security JWT (HS256)
- Rate limiting login/refresh
- Brute-force lock
- CORS configurable
- Security headers (HSTS, CSP, …)
- Actuator + Micrometer Prometheus
- Cache Caffeine
- OpenAPI (Swagger) hors profil `prod`

---

## 5. Architecture Service AI

**Dossier :** `flypro-ai/`

```text
app/
├── api/routers/      # Endpoints /api/v1/ai/*
├── ml/               # IsolationForest, features, preprocessor
├── services/         # training, prediction, registry, rules
├── schemas/          # Pydantic (camelCase)
├── models/           # Enums métier
└── core/             # config, logging, exceptions
artifacts/models/     # Joblib + latest.json (volume Docker)
```

- **Aucune base de données** : données fournies via REST
- Pipeline : features → StandardScaler → Isolation Forest → règles métier → fusion scores
- Gestion de versions horodatées des modèles

---

## 6. Architecture base de données

| Élément | Valeur |
|---------|--------|
| SGBD | MySQL 8.4 (InnoDB, utf8mb4) |
| Migrations | Flyway `V1` … `V10` |
| ORM | Hibernate (`ddl-auto=validate`) |
| Timezone | UTC |

Entités principales : utilisateurs/rôles, départements, employés, badges, affectations, zones, profils, points d’accès, événements, alertes, imports, scores d’anomalie, audit.

Détail : [DATABASE.md](DATABASE.md) · ERD : [UML/erd.puml](UML/erd.puml)

---

## 7. Architecture Docker

| Conteneur | Image | Port hôte | Volume |
|-----------|-------|-----------|--------|
| `flypro-frontend` | build multi-stage Node→Nginx | 80 | — |
| `flypro-backend` | Maven→JRE 21 | 8080 | logs |
| `flypro-ai` | Python 3.12 slim | 8000 | modèles |
| `flypro-mysql` | mysql:8.4 | 3306 | data |
| `flypro-adminer` | adminer:4 (profile `tools`) | 8081 | — |

**Ordre de démarrage :** MySQL healthy → AI → Backend (dépend MySQL + AI) → Frontend.

Nginx (`flypro-frontend/nginx.conf`) :

| Location | Cible |
|----------|--------|
| `/` | SPA |
| `/api/` | `backend:8080` |
| `/ai-api/` | `ai-service:8000` (strip préfixe) |
| `/actuator/health` | backend |
| `/healthz` | probe locale |

---

## 8. Flux de données métier (synthèse)

1. **Auth** — login → JWT → stockage SPA → Bearer sur chaque appel  
2. **Événement** — création → persistance → moteur d’alertes  
3. **Import** — upload → preview → execute → quality issues (+ alerte si besoin)  
4. **IA** — formulaire Analytics → `/predict` → risque / confiance / action  

Diagrammes détaillés : dossier [UML/](UML/).

---

## 9. Non-objectifs / périmètre

- Pas de synchronisation temps réel WebSocket (v1)
- Intégration backend→AI préparée (`AI_SERVICE_URL`) ; la page Analytics appelle l’IA directement
- Le dossier `flypro-analytics/` est un **squelette** monorepo (PostgreSQL) non utilisé comme stack de livraison

---

## 10. Références

- [INSTALLATION.md](INSTALLATION.md)
- [DEPLOYMENT.md](DEPLOYMENT.md)
- [SECURITY.md](SECURITY.md)
- [AI.md](AI.md)
- README racine du dépôt
