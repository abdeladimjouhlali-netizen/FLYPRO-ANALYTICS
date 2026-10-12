# Déploiement – FlyPro Analytics

## 1. Objectif

Décrire le déploiement **développement** et **production/démonstration** via Docker Compose, Nginx et variables d’environnement.

## 2. Docker

### Images

| Service | Dockerfile | Stratégie |
|---------|------------|-----------|
| Backend | `flypro-backend/Dockerfile` | Multi-stage Maven → JRE Alpine, user non-root |
| AI | `flypro-ai/Dockerfile` | Multi-stage pip → runtime slim, user `flypro` |
| Frontend | `flypro-frontend/Dockerfile` | Multi-stage Node build → Nginx |

### Bonnes pratiques appliquées

- Layers cache (dépendances avant sources)
- Healthchecks
- `restart: unless-stopped`
- Volumes pour MySQL, logs backend, modèles AI
- Pas de secrets dans l’image

## 3. Docker Compose

Fichier racine : `docker-compose.yml` (`name: flypro-analytics`)

Services : `mysql`, `backend`, `ai-service`, `frontend`, `adminer` (profile `tools`).

Ordre de démarrage :

1. MySQL healthy  
2. AI started / healthy  
3. Backend healthy (dépend MySQL + AI)  
4. Frontend (dépend backend + AI healthy)

## 4. Nginx

Fichier : `flypro-frontend/nginx.conf`

| Location | Cible |
|----------|--------|
| `/` | SPA (`index.html`) |
| `/api/` | `backend:8080` |
| `/ai-api/` | `ai-service:8000` |
| `/actuator/health` | backend (uniquement health) |
| `/actuator/info` | backend |
| `/assets/` | cache immutable 1 an |
| `/healthz` | probe local |

Headers de sécurité : CSP, X-Frame-Options, nosniff, Referrer-Policy, Permissions-Policy.  
`client_max_body_size 55m` (imports).

## 5. Variables d’environnement

Référence : `.env.example`

### Ports hôtes

`FRONTEND_PORT`, `BACKEND_PORT`, `AI_PORT`, `MYSQL_PORT`, `ADMINER_PORT`

### MySQL / JDBC

`MYSQL_*`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`

### Sécurité Spring

`JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `JWT_ISSUER`, `CORS_ALLOWED_ORIGINS`

### Rate limit / pool

`AUTH_RATE_LIMIT_*`, `AUTH_BRUTE_FORCE_*`, `DB_POOL_SIZE`, `DB_POOL_MIN_IDLE`

### Frontend build-args

`VITE_API_BASE_URL` (vide = same-origin)  
`VITE_AI_BASE_URL` (`/ai-api`)

### AI

`AI_LOG_LEVEL`, `CORS_ALLOWED_ORIGINS`, `MODELS_DIR`

## 6. Déploiement développement

### Option A – Compose local

```bash
cp .env.example .env
docker compose up --build
```

Swagger backend accessible sur `:8080` si profil non-prod ; sous Compose le profil est `prod` → Swagger désactivé.

### Option B – Processus locaux

MySQL + `mvn spring-boot:run` (profil `dev`) + `uvicorn` + `npm run dev`.

## 7. Déploiement production / démo PFE

1. VM / serveur avec Docker  
2. Copier le projet + `.env` durci  
3. `docker compose up --build -d`  
4. Terminer TLS devant le port 80 (Traefik, Nginx, LB cloud)  
5. Ne publier que le gateway (fermer 8080/8000/3306 vers Internet)  
6. Sauvegarder le volume `flypro_mysql_data`  
7. Monitorer : `docker compose logs -f`, Actuator health, Prometheus interne

### Checklist production

- [ ] `JWT_SECRET` unique et long  
- [ ] Mots de passe DB changés  
- [ ] CORS restreint au domaine public  
- [ ] TLS + HSTS au edge  
- [ ] Backups MySQL planifiés  
- [ ] Ports internes non exposés

## 8. Observabilité

| Endpoint | Usage |
|----------|--------|
| `/actuator/health` | Liveness / readiness |
| `/actuator/info` | Métadonnées app |
| `/actuator/prometheus` | Metrics (rôle ADMIN+) |
| `/api/v1/ai/health` | État modèle IA |

Voir `monitoring/README.md` et [QUALITY_REPORT.md](QUALITY_REPORT.md).

## 9. Rollback

```bash
docker compose down
# restaurer volume / image précédente
docker compose up -d
```

Flyway ne downgrade pas automatiquement : préparer un plan SQL avant rollback de schéma.
