# Documentation API – FlyPro Analytics

**Backend base URL :** `/api/v1`  
**AI base URL :** `/api/v1/ai` (via gateway : `/ai-api/api/v1/ai/...`)  
**Format :** JSON (`application/json`) sauf imports multipart.

---

## Conventions

### Authentification

Header : `Authorization: Bearer <accessToken>`  
Exceptions publiques : `POST /auth/login`, `/auth/refresh`, `/auth/logout`, Actuator health/info, Swagger (dev).

### Enveloppe succès

```json
{
  "success": true,
  "message": "...",
  "data": { }
}
```

### Erreurs courantes

| HTTP | Code (ex.) | Signification |
|------|------------|---------------|
| 400 | VALIDATION_ERROR, BAD_REQUEST | Requête invalide |
| 401 | UNAUTHORIZED | Non authentifié / credentials |
| 403 | ACCESS_DENIED | Rôle insuffisant |
| 404 | *_NOT_FOUND | Ressource absente |
| 409 | CONFLICT | Conflit métier |
| 429 | RATE_LIMIT_EXCEEDED, ACCOUNT_TEMPORARILY_LOCKED | Limite / lock |
| 500 | INTERNAL_ERROR | Erreur serveur |

Exemple :

```json
{
  "timestamp": "2026-07-27T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/...",
  "fieldErrors": []
}
```

### Rôles

`SUPER_ADMIN`, `ADMIN`, `ANALYST` (Spring `ROLE_*`).  
Abréviation ci-dessous : **SA** = SUPER_ADMIN.

### Pagination

Paramètres Spring Data typiques : `page`, `size`, `sort`.  
Réponse : `PageResponse` (content, totalElements, totalPages, …).

---

## 1. Authentication APIs — `/api/v1/auth`

| Méthode | Path | Auth | Purpose |
|---------|------|------|---------|
| POST | `/login` | Public | Authentification |
| POST | `/refresh` | Public | Renouvellement tokens |
| POST | `/logout` | Public / Bearer | Révocation refresh |
| GET | `/me` | Bearer | Profil courant |

### POST `/login`

**Purpose :** Obtenir access + refresh JWT.  
**Request :**

```json
{ "username": "admin", "password": "Admin@123" }
```

**Response 200 :** `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn`, `user`.  
**Errors :** 401 credentials ; 429 rate-limit / account locked.

### POST `/refresh`

**Request :** `{ "refreshToken": "..." }`  
**Response :** nouveaux tokens.  
**Errors :** 400 token invalide / révoqué / expiré.

### POST `/logout`

**Request :** `{ "refreshToken": "..." }` (optionnel selon implémentation)  
**Response :** 204/200 succès.  
**Purpose :** Révoquer le refresh token côté serveur.

### GET `/me`

**Response :** `UserResponse` (id, username, email, enabled, accountLocked, role, timestamps).  
**Errors :** 401.

---

## 2. Department APIs — `/api/v1/departments`

| Méthode | Path | Auth | Purpose |
|---------|------|------|---------|
| GET | `/` | Authentifié | Recherche paginée |
| GET | `/{id}` | Authentifié | Détail |
| POST | `/` | Authentifié | Création |
| PUT | `/{id}` | Authentifié | Mise à jour |
| DELETE | `/{id}` | Authentifié | Delete / soft-deactivate |

**Query (GET) :** `name`, `code`, `active`, `createdFrom`, `createdTo`, pageable.  
**Request body :** `{ "name", "code", "description" }`  
**Response :** `DepartmentResponse`  
**Errors :** 404 ; 400 duplicate code/name.  
> Note : pas de `@PreAuthorize` granulaire sur ce contrôleur — session authentifiée requise.

---

## 3. Employee APIs — `/api/v1/employees`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/` | SA, ADMIN, ANALYST | Recherche |
| GET | `/{id}` | idem | Détail |
| POST | `/` | SA, ADMIN | Création |
| PUT | `/{id}` | SA, ADMIN | Mise à jour |
| PATCH | `/{id}/deactivate` | SA, ADMIN | → INACTIVE |
| PATCH | `/{id}/activate` | SA, ADMIN | → ACTIVE |
| PATCH | `/{id}/archive` | SA, ADMIN | → ARCHIVED |
| DELETE | `/{id}` | SA, ADMIN | Suppression |
| GET | `/{employeeId}/badges` | SA, ADMIN, ANALYST | Badges liés |

**Query :** `employeeNumber`, `firstName`, `lastName`, `departmentId`, `status`, `position`, `email`, `keyword`.  
**Request :** `EmployeeRequestDTO` (matricule, identité, email, phone, position, status, departmentId, accessProfileId).  
**Errors :** 404 ; 400 validation / règles cycle de vie ; 403.

---

## 4. Badge APIs — `/api/v1/badges`

| Méthode | Path | Rôles lecture / écriture | Purpose |
|---------|------|--------------------------|---------|
| GET | `/` | ANALYST+ | Liste |
| GET | `/{id}` | ANALYST+ | Détail |
| POST | `/` | ADMIN+ | Création |
| PUT | `/{id}` | ADMIN+ | Update |
| DELETE | `/{id}` | ADMIN+ | Suppression |
| POST | `/{id}/assign/{employeeId}` | ADMIN+ | Affectation |
| PATCH | `/{id}/unassign` | ADMIN+ | Désaffectation |
| PATCH | `/{id}/renew` | ADMIN+ | Renouvellement |
| PATCH | `/{id}/replace` | ADMIN+ | Remplacement |
| PATCH | `/{id}/suspend` | ADMIN+ | Suspension |
| PATCH | `/{id}/activate` | ADMIN+ | Activation |
| PATCH | `/{id}/revoke` | ADMIN+ | Révocation |
| PATCH | `/{id}/lost` | ADMIN+ | Perdu |
| GET | `/{id}/history` | ANALYST+ | Historique |

**Bodies notables :**

- Create/Update : `badgeNumber`, `type`, `status`, `issueDate`, `expirationDate`
- Renew : `expirationDate`, `issueDate?`
- Replace : `newBadgeId`

**Errors :** 400 règles métier (déjà assigné, expiré…) ; 404 ; 403.

---

## 5. Zone APIs — `/api/v1/zones`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/` | ANALYST+ | Recherche |
| GET | `/{id}` | ANALYST+ | Détail |
| POST | `/` | ADMIN+ | Création |
| PUT | `/{id}` | ADMIN+ | Update |
| DELETE | `/{id}` | ADMIN+ | Suppression |
| GET | `/{zoneId}/access-points` | ANALYST+ | Points de la zone |

**Request :** `name`, `building`, `floor`, `sensitive`, `description`  
**Errors :** 400 si dépendances (points / profils) ; 404.

---

## 6. Access Profile APIs — `/api/v1/access-profiles`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/` | ANALYST+ | Liste |
| GET | `/{id}` | ANALYST+ | Détail |
| POST | `/` | ADMIN+ | Création |
| PUT | `/{id}` | ADMIN+ | Update |
| DELETE | `/{id}` | ADMIN+ | Suppression |
| POST | `/{id}/zones/{zoneId}` | ADMIN+ | Lier zone |
| DELETE | `/{id}/zones/{zoneId}` | ADMIN+ | Délier zone |

**Request :** `name`, `description`, `securityLevel`, `startTime`, `endTime`, `zoneIds`  
**Errors :** 400 (min 1 zone, employés liés) ; 404.

---

## 7. Access Point APIs — `/api/v1/access-points`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/` | ANALYST+ | Recherche |
| GET | `/{id}` | ANALYST+ | Détail |
| POST | `/` | ADMIN+ | Création |
| PUT | `/{id}` | ADMIN+ | Update |
| DELETE | `/{id}` | ADMIN+ | Suppression |
| PATCH | `/{id}/activate` | ADMIN+ | Activer |
| PATCH | `/{id}/deactivate` | ADMIN+ | Désactiver |
| PATCH | `/{id}/maintenance` | ADMIN+ | Maintenance |

**Request :** `name`, `code`, `type`, `description`, `status`, `ipAddress`, `location`, `zoneId`  
**Errors :** 400 si événements liés au delete ; 404.

---

## 8. Access Event APIs — `/api/v1/access-events`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/` | ANALYST+ | Recherche filtrée |
| GET | `/{id}` | ANALYST+ | Détail |
| POST | `/` | ADMIN+ | Création manuelle |

**Request :** `eventTimestamp`, `direction`, `result`, `refusalReason`, `source`, `rawPayload`, `badgeId`, `accessPointId`, `zoneId`  
**Notes :** Append-only ; peut générer des alertes.  
**Errors :** 400 validation FK ; 404.

---

## 9. Import APIs — `/api/v1/imports`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| POST | `/upload` | ADMIN+ | Upload multipart |
| POST | `/preview` | ADMIN+ | Preview fichier |
| POST | `/preview/{batchId}` | ADMIN+ | Preview batch |
| POST | `/execute` | ADMIN+ | Exécution JSON ou multipart |
| GET | `/` | ANALYST+ | Liste lots |
| GET | `/{id}` | ANALYST+ | Détail |
| GET | `/{id}/report` | ANALYST+ | Rapport qualité |

**Formats :** CSV, XLSX, JSON — max ~50 MB.  
**Errors :** UNSUPPORTED_FILE_TYPE, INVALID_FILE_FORMAT, 413 taille.

---

## 10. Alert APIs — `/api/v1`

| Méthode | Path | Rôles | Purpose |
|---------|------|-------|---------|
| GET | `/alerts` | ANALYST+ | Liste filtrée |
| GET | `/alerts/{id}` | ANALYST+ | Détail |
| PATCH | `/alerts/{id}/acknowledge` | ADMIN+ | OPEN → ACKNOWLEDGED |
| PATCH | `/alerts/{id}/resolve` | ADMIN+ | → RESOLVED |
| PATCH | `/alerts/{id}/close` | ADMIN+ | → CLOSED |
| GET | `/employees/{employeeId}/alerts` | ANALYST+ | Par employé |
| GET | `/badges/{badgeId}/alerts` | ANALYST+ | Par badge |

**Errors :** 400 transition invalide ; 404.

---

## 11. Dashboard APIs — `/api/v1/dashboard`

Tous en **GET**, rôles **SA, ADMIN, ANALYST**.

Filtres courants : `from`, `to`, `departmentId`, `zoneId`, `accessPointId`, `employeeId`, `badgeId`, `alertSeverity`, `alertType`.

| Path | Response | Purpose |
|------|----------|---------|
| `/kpis` | `List<KpiDTO>` | Indicateurs clés |
| `/charts` | `List<ChartDTO>` | Séries graphiques |
| `/statistics` | `StatisticsDTO` | Stats agrégées |
| `/trends` | `List<TrendDTO>` | Tendances |
| `/top-access-points` | `List<NamedCountDTO>` | Top lecteurs (+ `limit`) |
| `/top-zones` | idem | Top zones |
| `/top-employees` | idem | Top employés |
| `/peak-hours` | buckets 24h | Heures de pointe UTC |

**Errors :** 401 / 403.

---

## 12. AI APIs — `/api/v1/ai` (FastAPI)

Pas de JWT imposé par le service AI (à protéger au réseau / gateway en prod).

| Méthode | Path | Purpose |
|---------|------|---------|
| GET | `/health` | Santé + modèle chargé |
| GET | `/model` | Métadonnées modèle |
| POST | `/model/load?version=` | Charger une version |
| POST | `/model/save` | Persister modèle courant |
| POST | `/train` | Entraîner (201) |
| POST | `/retrain` | Réentraîner (201) |
| POST | `/evaluate` | Métriques sur dataset |
| POST | `/predict` | Prédiction unitaire |
| POST | `/predict/batch` | Batch (1–500) |

### POST `/predict`

**Request :**

```json
{
  "event": {
    "eventTimestamp": "2026-07-27T22:15:00Z",
    "employeeId": 1,
    "badgeId": 1,
    "accessPointId": 1,
    "zoneId": 1,
    "accessResult": "DENIED",
    "accessDirection": "ENTRY",
    "deniedLastHour": 3,
    "accessesToday": 12,
    "employeeStatus": "ACTIVE",
    "badgeStatus": "ACTIVE",
    "isExpectedZone": false
  }
}
```

**Response data :**

```json
{
  "prediction": "ANOMALY",
  "riskScore": 0.94,
  "confidence": 98.5,
  "reason": "...",
  "recommendedAction": "INVESTIGATE",
  "modelVersion": "20260727120000",
  "ruleFlags": ["OUTSIDE_WORKING_HOURS", "REPEATED_DENIED_ACCESSES"],
  "anomalyScore": -0.12
}
```

**Errors AI :** `MODEL_NOT_LOADED`, `VALIDATION_ERROR` (422), `INSUFFICIENT_SAMPLES` (train).

---

## 13. Actuator

| Path | Auth | Notes |
|------|------|-------|
| `/actuator/health` | Public | Probes |
| `/actuator/info` | Public | Info |
| `/actuator/prometheus` | ADMIN+ | Metrics |
| `/actuator/metrics` | Selon config | |

Swagger UI : `/swagger-ui.html` — **désactivé en `prod`**.  
OpenAPI AI : http://localhost:8000/docs
