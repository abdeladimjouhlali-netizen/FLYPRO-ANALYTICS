# FlyPro Analytics

Plateforme intelligente de **gestion des badges et des accès** (projet de fin d'études).

Ce dépôt est un **monorepo** initialisé selon les principes **Clean Architecture** et **Domain-Driven Design (DDD)**.  
À ce stade : **squelette d'architecture uniquement** — aucune logique métier, pas de CRUD, pas d'authentification, pas d'entités.

---

## Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│    Frontend     │────▶│   Backend Spring     │────▶│   PostgreSQL    │
│  React + Vite   │     │  Boot (modulaire)    │     │                 │
└─────────────────┘     └──────────┬───────────┘     └─────────────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │   AI Service         │
                        │   FastAPI + ML       │
                        └──────────────────────┘
```

### Backend (Clean Architecture / DDD)

Chaque module métier suit les mêmes couches :

| Couche | Rôle |
|--------|------|
| `controller` | Adapters REST (ports d'entrée) |
| `service` | Cas d'utilisation |
| `repository` | Ports de persistance |
| `entity` | Modèle de domaine persisté |
| `dto` | Contrats API |
| `mapper` | MapStruct Entity ↔ DTO |
| `validator` | Règles de validation |
| `specification` | Filtres JPA dynamiques |
| `exception` | Exceptions du module |

Packages transverses : `config`, `security`, `exception`, `common` (constants, enums, utils, response, pagination).

### Frontend (feature-based)

Organisation par **features** (`employees`, `badges`, `dashboard`, …), chacune avec `components`, `pages`, `hooks`, `services`, `types`.  
Couches partagées : `api`, `routes`, `layouts`, `components` (Shadcn UI), etc.

### AI Service

Microservice FastAPI dédié à la **détection d'anomalies** (Pandas, NumPy, Scikit-learn), appelé par le module `analytics` du backend.

---

## Technologies

| Couche | Stack |
|--------|--------|
| Backend | Java 21, Spring Boot 3.5, Security, Data JPA, Validation, Lombok, MapStruct, Flyway, PostgreSQL |
| Frontend | React, TypeScript, Vite, React Router, Axios, Tailwind CSS, Shadcn UI, React Hook Form, Zod, TanStack Query |
| AI | Python, FastAPI, Pandas, NumPy, Scikit-learn |
| Infra | Docker, Docker Compose |

---

## Structure du monorepo

```
flypro-analytics/
├── backend/                 # Spring Boot (Maven)
│   └── src/main/java/com/flypro/analytics/
│       ├── config/          # OpenApi, Security, Jackson, Cors
│       ├── security/
│       ├── exception/
│       ├── common/
│       ├── audit/ … notification/   # modules métier (packages vides)
│       └── FlyproAnalyticsApplication.java
├── frontend/                # React + Vite + TypeScript
│   └── src/
│       ├── features/        # modules UI par domaine
│       ├── api/ components/ layouts/ …
│       └── App.tsx          # placeholder
├── ai-service/              # FastAPI
│   ├── app/                 # api, core, models, schemas, services, ml, …
│   ├── main.py
│   └── requirements.txt
├── docker/                  # Dockerfiles + nginx.conf
├── docs/                    # documentation architecture
├── scripts/                 # helpers dev
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

## Prérequis

- **JDK 21** + Maven 3.9+
- **Node.js 20+** (recommandé 22) + npm
- **Python 3.12+**
- **Docker** + Docker Compose

---

## Démarrage

### Option A — Stack complète (Docker)

```bash
cd flypro-analytics
docker compose up -d --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| AI Service | http://localhost:8000/health |
| PostgreSQL | localhost:5432 |

Arrêt :

```bash
docker compose down
```

### Option B — Développement local (services séparés)

**1. PostgreSQL**

```bash
docker compose up -d postgres
```

**2. Backend**

```bash
cd backend
./mvnw spring-boot:run
# ou : mvn spring-boot:run
```

**3. Frontend**

```bash
cd frontend
npm install
npm run dev
```

→ http://localhost:5173

**4. AI Service**

```bash
cd ai-service
python -m venv .venv
# Windows: .venv\Scripts\activate
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

→ http://localhost:8000/health

---

## Roadmap de développement

L'implémentation se fera **module par module** (pas de big bang) :

1. **Socle commun** — réponses API, pagination, exceptions globales, CORS, OpenAPI  
2. **Sécurité & utilisateurs** — auth, rôles, audit  
3. **Organisation** — departments, employees  
4. **Accès physique** — zones, access points, access profiles, badges  
5. **Événements** — access events, imports CSV/JSON  
6. **Pilotage** — dashboard, alerts, notifications  
7. **Intelligence** — module analytics + pipelines ML (FastAPI)  
8. **Durcissement** — tests, observabilité, CI/CD, déploiement prod  

---

## Conventions

- **Backend** : packages par domaine ; pas de logique dans les controllers ; DTO ≠ Entity.  
- **Frontend** : features isolées ; appels HTTP via `api/` + services de feature ; validation Zod.  
- **AI** : schemas Pydantic séparés des services ML ; modèles versionnés hors Git (artefacts).  
- **Migrations** : uniquement via Flyway (`backend/src/main/resources/db/migration/`).

---

## État actuel

| Élément | Statut |
|---------|--------|
| Structure monorepo | ✅ |
| Packages / dossiers | ✅ |
| Config classes (vides) | ✅ |
| Docker / Compose | ✅ |
| Entités / CRUD / Auth | ❌ volontairement absent |
| Logique métier | ❌ volontairement absent |

---

## Licence / Projet

Projet académique **FlyPro Analytics** — Plateforme de gestion intelligente des badges employés.
