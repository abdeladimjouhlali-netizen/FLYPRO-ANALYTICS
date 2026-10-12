# Guide d’installation – FlyPro Analytics

**Version :** 1.0.0  
**Objectif :** démarrer la plateforme en local (Docker ou mode développement).

---

## 1. Prérequis

### Obligatoires (Docker)

| Outil | Version minimale |
|-------|------------------|
| Docker Engine / Desktop | 24+ |
| Docker Compose | v2 |

### Optionnels (développement hors Docker)

| Outil | Version |
|-------|---------|
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 22 LTS |
| npm | 10+ |
| Python | 3.12 |
| MySQL | 8.4 (si DB hors Compose) |

Ressources recommandées : **4 Go RAM** libre, **10 Go** disque.

---

## 2. Cloner le projet

```bash
git clone <url-du-depot> "pfe-Ram"
cd "pfe-Ram"
```

Structure utile :

```text
.
├── docker-compose.yml
├── .env.example
├── scripts/
├── docs/
├── flypro-backend/
├── flypro-frontend/
└── flypro-ai/
```

---

## 3. Configurer l’environnement

```bash
# Linux / macOS
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Éditer `.env` et **durcir** au minimum :

| Variable | Règle |
|----------|--------|
| `MYSQL_ROOT_PASSWORD` | Mot de passe fort |
| `MYSQL_PASSWORD` | Mot de passe fort |
| `JWT_SECRET` | ≥ 32 caractères aléatoires |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées |

Valeurs de démo acceptables uniquement en local non exposé.

---

## 4. Lancer avec Docker (recommandé)

### Démarrage complet

```bash
docker compose up --build -d
```

Ou scripts :

```bash
# Linux / macOS
chmod +x scripts/*.sh
./scripts/deploy.sh

# Windows PowerShell
./scripts/deploy.ps1
```

### Vérification

| URL | Attendu |
|-----|---------|
| http://localhost/ | SPA FlyPro |
| http://localhost/healthz | `ok` |
| http://localhost:8080/actuator/health | `"status":"UP"` |
| http://localhost:8000/api/v1/ai/health | JSON health |
| http://localhost:8000/docs | OpenAPI FastAPI |

Compte seed (Flyway V2) :

| Champ | Valeur |
|-------|--------|
| Username | `admin` |
| Password | `Admin@123` |
| Rôle | `ROLE_SUPER_ADMIN` |

### Arrêt

```bash
docker compose down
# Conserver les données MySQL (volumes) par défaut
# Reset complet :
docker compose down -v
```

### Adminer (optionnel)

```bash
docker compose --profile tools up -d adminer
# http://localhost:8081
```

---

## 5. Installation mode développement

Lancer MySQL (Compose partiel ou instance locale), puis chaque service.

### 5.1 Base de données

```bash
docker compose up -d mysql
# DATABASE_URL typique :
# jdbc:mysql://localhost:3306/flypro_analytics?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

### 5.2 Backend Spring Boot

```bash
cd flypro-backend
cp .env.example .env   # si besoin
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- API : http://localhost:8080  
- Swagger : http://localhost:8080/swagger-ui.html  

### 5.3 Service AI

```bash
cd flypro-ai
python -m venv .venv

# Windows
.\.venv\Scripts\Activate.ps1
# Linux/macOS
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

- Docs : http://localhost:8000/docs  

### 5.4 Frontend React

```bash
cd flypro-frontend
cp .env.example .env
# VITE_API_BASE_URL=http://localhost:8080
# VITE_AI_BASE_URL=http://localhost:8000
npm install
npm run dev
```

- UI : http://localhost:5173  

Le proxy Vite relaie `/api` et `/ai-api` si configuré.

---

## 6. Smoke tests

```bash
# Après Compose
./scripts/smoke-test.sh
```

Contrôles manuels :

1. Login `admin` / `Admin@123`
2. Dashboard affiche des KPI (éventuellement vides)
3. Créer un département de test
4. Page Analytics → health AI `modelLoaded` true/false

---

## 7. Dépannage rapide

| Symptôme | Action |
|----------|--------|
| Backend unhealthy | `docker compose logs backend` — souvent DB pas prête ou JWT_SECRET |
| 401 permanent | Vérifier CORS + horloge système + token expiré |
| Frontend blanc | Rebuild avec `VITE_*` ; hard refresh |
| AI 503 modèle | Entraîner via `/api/v1/ai/train` ou ignorer si preview only |
| Port déjà utilisé | Changer `FRONTEND_PORT` / `BACKEND_PORT` dans `.env` |

---

## 8. Prochaines étapes

- Déploiement : [DEPLOYMENT.md](DEPLOYMENT.md)
- Architecture : [ARCHITECTURE.md](ARCHITECTURE.md)
- Manuel utilisateur : [USER_GUIDE.md](USER_GUIDE.md)
