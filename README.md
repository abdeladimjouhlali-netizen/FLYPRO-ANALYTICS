# =============================================================================
# FlyPro Analytics
# Plateforme intelligente de gestion des badges & accès employés
# =============================================================================

## Project Overview

**FlyPro Analytics** is a Final Year Project (PFE) platform for employee badge access
management, operational dashboards, alerting and AI-assisted anomaly detection.

The system is composed of three applications orchestrated with Docker Compose:

| Service | Technology | Role |
|---------|------------|------|
| `frontend` | React 19 + Vite + Nginx | SPA + reverse proxy |
| `backend` | Spring Boot 3.5 + Java 21 | REST API, security, domain |
| `ai-service` | FastAPI + Isolation Forest | Anomaly detection microservice |
| `mysql` | MySQL 8.4 | Persistent relational store |
| `redis` / `rabbitmq` | Redis 7 / RabbitMQ 3.13 | HA overlay (`docker-compose.ha.yml`) |

> **Database note:** the delivered Spring Boot backend uses **MySQL** (Flyway migrations + MySQL dialect).
> Compose therefore runs MySQL so `docker compose up --build` starts a working system.
> PostgreSQL HA templates live under `infra/ha/postgresql/` for a future migration path.

## Architecture Diagram

```text
┌──────────────┐      ┌────────────────────┐      ┌──────────────────┐
│   Browser    │─────▶│  Nginx (frontend)  │─────▶│ Spring Boot API  │
└──────────────┘      │  :80               │      │ :8080            │
                      │  /api  → backend   │      └────────┬─────────┘
                      │  /ai-api → AI      │               │
                      └─────────┬──────────┘               ▼
                                │                   ┌──────────────┐
                                └──────────────────▶│   MySQL 8    │
                                                    └──────────────┘
                                ┌──────────────────┐
                                │ FastAPI AI :8000 │
                                └──────────────────┘
```

## Technology Stack

- **Frontend:** React 19, TypeScript, Vite, Tailwind, TanStack Query, Recharts
- **Backend:** Spring Boot 3.5, Spring Security JWT, JPA, Flyway, MapStruct, Swagger
- **AI:** Python 3.12, FastAPI, Scikit-Learn, Joblib
- **Infra:** Docker, Docker Compose, Nginx, Kubernetes, GitHub Actions
- **HA:** Redis, RabbitMQ, Resilience4j, Nginx LB / API Gateway, OpenTelemetry / Jaeger
- **DB:** MySQL 8.4 (Adminer optional profile; replica config under `infra/ha/mysql/`)

## Folder Structure

```text
.
├── docker-compose.yml
├── .env.example
├── .github/workflows/ci.yml
├── k8s/                     # Kubernetes manifests (Phase 5–6)
│   ├── namespace.yaml
│   ├── secret.yaml / configmap.yaml
│   ├── *-deployment.yaml / services.yaml / ingress.yaml / hpa.yaml
│   ├── redis-rabbitmq.yaml
│   ├── prometheus.yml / grafana-dashboard.json
│   ├── backup.sh / restore.sh
│   └── monitoring / logging / tracing stacks
├── infra/                   # HA enterprise (Phase 6 – Step 20)
│   ├── nginx/               # Load balancer
│   ├── gateway/             # API Gateway Nginx
│   ├── ha/mysql|postgresql/ # DB HA templates
│   ├── redis/ · rabbitmq/
│   └── perf/                # k6 smoke / stress
├── docs/                    # Documentation PFE (Phases 5–6)
│   ├── README.md            # Index documentation
│   ├── ARCHITECTURE.md
│   ├── INSTALLATION.md
│   ├── DEPLOYMENT.md
│   ├── DATABASE.md
│   ├── API.md
│   ├── SECURITY.md
│   ├── AI.md
│   ├── USER_GUIDE.md
│   ├── MAINTENANCE.md
│   ├── PRESENTATION.md
│   ├── QUALITY_REPORT.md
│   └── UML/                 # Diagrammes PlantUML
├── monitoring/              # Prometheus scrape notes
├── scripts/
│   ├── deploy.sh / deploy.ps1
│   ├── wait-for-healthy.sh
│   └── smoke-test.sh
├── flypro-backend/          # Spring Boot API
├── flypro-frontend/         # React SPA (+ nginx.conf)
└── flypro-ai/               # FastAPI anomaly service
```

## Installation Guide

### Prerequisites

- Docker Desktop / Docker Engine **24+**
- Docker Compose v2
- (Optional local dev) JDK 21, Node 22, Python 3.12, Maven 3.9

### Clone & configure

```bash
cp .env.example .env
# Edit .env — set MYSQL_* passwords and JWT_SECRET (>= 32 chars)
```

## Docker Guide

### Start the full stack

```bash
docker compose up --build
```

Or:

```bash
# Linux/macOS
chmod +x scripts/*.sh
./scripts/deploy.sh

# Windows PowerShell
./scripts/deploy.ps1
```

### Stop

```bash
docker compose down
# with volumes:
docker compose down -v
```

### Optional DB UI (Adminer)

```bash
docker compose --profile tools up -d adminer
# http://localhost:8081
```

### High Availability overlay (Redis + RabbitMQ)

```bash
docker compose -f docker-compose.yml -f docker-compose.ha.yml up -d
# Management UI RabbitMQ: http://localhost:15672
```

Docs : [docs/HIGH_AVAILABILITY.md](docs/HIGH_AVAILABILITY.md)

## Configuration Guide / Environment Variables

See [`.env.example`](.env.example). Key variables:

| Variable | Description |
|----------|-------------|
| `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | Database credentials |
| `JWT_SECRET` | HS256 signing secret (≥ 32 chars) |
| `SPRING_PROFILES_ACTIVE` | Usually `prod` in Compose |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins |
| `AI_SERVICE_URL` | Backend → AI internal URL |
| `VITE_API_BASE_URL` | Frontend API base (empty = same origin) |
| `VITE_AI_BASE_URL` | Frontend AI base (`/ai-api` behind Nginx) |

**Never commit `.env` with real secrets.**

## Running Locally (without Docker)

1. Start MySQL and create DB `flypro_analytics`
2. Backend: `cd flypro-backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
3. AI: `cd flypro-ai && uvicorn app.main:app --reload --port 8000`
4. Frontend: `cd flypro-frontend && npm run dev`

## Running with Docker

```bash
docker compose up --build -d
```

Entrypoints:

| URL | Description |
|-----|-------------|
| http://localhost/ | React UI |
| http://localhost/api/v1/... | API via Nginx |
| http://localhost/ai-api/api/v1/ai/... | AI via Nginx |
| http://localhost:8080/swagger-ui.html | Spring Swagger |
| http://localhost:8000/docs | FastAPI Swagger |
| http://localhost:8080/actuator/health | Backend health |
| http://localhost:8000/api/v1/ai/health | AI health |

Comptes seed :
- Super Admin : `admin` / `Admin@123`
- Administrateur (métier, imports, dashboard) : `opsadmin` / `Admin@123`
- Analyste (lecture, dashboard, analytics) : `analyste` / `Admin@123`

## API Documentation

- **Backend Swagger:** `/swagger-ui.html` (direct `:8080` or via gateway if exposed)
- **OpenAPI JSON:** `/v3/api-docs`
- **AI docs:** `http://localhost:8000/docs`
- **AI health:** `GET /api/v1/ai/health`

## Health Checks

| Component | Endpoint / probe |
|-----------|------------------|
| Spring Boot | `GET /actuator/health` |
| FastAPI | `GET /api/v1/ai/health` |
| MySQL | Docker `healthcheck.sh` |
| Nginx | `GET /healthz` |

## Logging

- Spring: console pattern + optional file (`LOG_FILE`, rotation via `logging.file.max-*`)
- FastAPI: stdout structured logger (captured by Docker)
- Access logs: `docker compose logs -f backend|ai-service|frontend`

## CI/CD

GitHub Actions workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

On every push/PR:

1. Backend Maven tests + package  
2. Frontend Vitest + production build  
3. AI Pytest  
4. Docker Compose build + startup smoke tests (health + login)

## Security

- JWT secrets via environment only (HS256, ≥ 32 chars)
- BCrypt cost factor 12
- Rate limiting on login/refresh + temporary lock after failed attempts
- CORS restricted by `CORS_ALLOWED_ORIGINS` (backend + AI)
- Security headers (CSP, HSTS, nosniff, frame deny) on API and Nginx
- Actuator: public health/info only through gateway; Prometheus requires ADMIN+
- Swagger / OpenAPI disabled on `prod` profile
- Non-root containers; no secrets baked into images

See [docs/QUALITY_REPORT.md](docs/QUALITY_REPORT.md).

## Monitoring

- Spring Boot Actuator + Micrometer Prometheus (`/actuator/prometheus`)
- Health probes (liveness/readiness)
- Structured logging + optional file rotation (`LOG_FILE`)
- Notes: [monitoring/README.md](monitoring/README.md)

## Architecture & Maintenance Docs

Documentation PFE complète (Phase 5 – Step 17) : **[docs/README.md](docs/README.md)**

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — architecture, flux, Docker  
- [docs/INSTALLATION.md](docs/INSTALLATION.md) — installation  
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — déploiement  
- [docs/DATABASE.md](docs/DATABASE.md) — modèle de données  
- [docs/API.md](docs/API.md) — catalogue API  
- [docs/SECURITY.md](docs/SECURITY.md) — sécurité  
- [docs/AI.md](docs/AI.md) — service IA  
- [docs/USER_GUIDE.md](docs/USER_GUIDE.md) — manuel utilisateur  
- [docs/MAINTENANCE.md](docs/MAINTENANCE.md) — maintenance développeur  
- [docs/PRESENTATION.md](docs/PRESENTATION.md) — support soutenance  
- [docs/QUALITY_REPORT.md](docs/QUALITY_REPORT.md) — audit production (Step 16)  
- [docs/UML/](docs/UML/) — sources PlantUML  
- [docs/HIGH_AVAILABILITY.md](docs/HIGH_AVAILABILITY.md) — HA enterprise (Step 20)  
- [docs/REDIS.md](docs/REDIS.md) · [docs/RABBITMQ.md](docs/RABBITMQ.md) · [docs/RESILIENCE4J.md](docs/RESILIENCE4J.md)  
- [docs/API_GATEWAY.md](docs/API_GATEWAY.md) · [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md)  

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Backend not healthy | `docker compose logs backend` — DB URL / Flyway / JWT_SECRET length |
| Frontend API 404 | Ensure Nginx proxies `/api/` and frontend built with empty `VITE_API_BASE_URL` |
| AI offline | `docker compose logs ai-service` — wait for model folder permissions |
| Login fails / 429 | Seed user + migrations; or rate-limit / brute-force lock window |
| Port busy | Change `FRONTEND_PORT` / `BACKEND_PORT` / `AI_PORT` in `.env` |
| Swagger missing in Docker | Expected on `prod` — use `dev` profile locally |

More detail: [docs/MAINTENANCE.md](docs/MAINTENANCE.md).

## Deployment Guide

1. Provision a VM/host with Docker  
2. Copy project + create hardened `.env`  
3. `docker compose up --build -d`  
4. Put a TLS terminator (Traefik/Nginx/Cloud LB) in front of port 80  
5. Restrict published ports (prefer exposing only the gateway)  
6. Backup volume `flypro_mysql_data` regularly  

## Future Improvements

- PostgreSQL runtime migration (templates already under `infra/ha/postgresql/`)  
- Redis Sentinel / RabbitMQ quorum cluster in managed cloud  
- Dual-key JWT rotation automation  
- Full domain worker consumers on RabbitMQ queues  

## License / Academic use

Designed for a university Final Year Project (PFE) demonstration and evaluation.
