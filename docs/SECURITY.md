# Documentation sécurité – FlyPro Analytics

**Version :** 1.0.0  
**Périmètre :** authentification, autorisation, durcissement HTTP, bonnes pratiques.

---

## 1. JWT Authentication

### Mécanisme

- Bibliothèque : **JJWT**
- Algorithme : **HS256**
- Issuer : `flypro-analytics` (configurable)
- Claims : `uid`, `role`, `type` (`ACCESS` | `REFRESH`)

| Token | Durée défaut | Stockage |
|-------|--------------|----------|
| Access | 15 min (`900000` ms) | Client (localStorage) |
| Refresh | 7 jours (`604800000` ms) | Client + table `refresh_tokens` |

### Flux

1. `POST /api/v1/auth/login` → access + refresh  
2. Requêtes API : `Authorization: Bearer <access>`  
3. `JwtAuthenticationFilter` valide signature, expiration, type ACCESS  
4. Expiration access → `POST /auth/refresh` (rotation)  
5. Logout → révocation refresh (`revoked=true`)

Détail UML : [UML/auth-flow.puml](UML/auth-flow.puml)

### Secret

Variable `JWT_SECRET` — **≥ 32 caractères**, unique par environnement, jamais commitée.

---

## 2. Role Based Access Control (RBAC)

### Rôles

| Rôle | Description |
|------|-------------|
| `ROLE_SUPER_ADMIN` | Administration complète + users/roles UI |
| `ROLE_ADMIN` | Écriture métier + imports + alertes |
| `ROLE_ANALYST` | Lecture / analyse / dashboard |
| `ROLE_EMPLOYEE` | Présent en seed ; peu d’accès UI métier |

### Application

- Méthodes annotées `@PreAuthorize("hasAnyRole('ADMIN', …)")`
- Spring retire le préfixe `ROLE_` dans `hasAnyRole`
- Frontend : `ProtectedRoute` + filtre sidebar

### Matrice simplifiée

| Capacité | Super Admin | Admin | Analyste |
|----------|:-----------:|:-----:|:--------:|
| Dashboard / lecture | ✓ | ✓ | ✓ |
| CRUD employés / badges | ✓ | ✓ | lecture |
| Imports | ✓ | ✓ | ✗ |
| Ack / resolve alertes | ✓ | ✓ | lecture |
| Users / Roles UI | ✓ | ✗ | ✗ |
| Analytics AI | ✓ | ✓ | ✓ |

---

## 3. Password Encryption

- Encodeur : **`BCryptPasswordEncoder`** avec **force 12**
- Aucun mot de passe en clair en base
- Compte seed V2 : `admin` / `Admin@123` — **à changer en production**

---

## 4. Security Headers

Configurés dans `SecurityConfig` (Spring) et renforcés dans Nginx :

| Header | Valeur / comportement |
|--------|------------------------|
| X-Content-Type-Options | `nosniff` |
| X-Frame-Options | `DENY` |
| Content-Security-Policy | restrictive (`default-src 'none'` côté API ; CSP Nginx pour SPA) |
| Referrer-Policy | `STRICT_ORIGIN_WHEN_CROSS_ORIGIN` |
| Strict-Transport-Security | 1 an, includeSubDomains, preload (HTTPS) |
| Permissions-Policy | géoloc / micro / caméra désactivés |
| server_tokens | `off` (Nginx) |

---

## 5. CORS

Classe `CorsConfig` + propriété `flypro.cors.allowed-origins` / `CORS_ALLOWED_ORIGINS`.

| Paramètre | Valeur typique |
|-----------|----------------|
| Origins | `http://localhost:5173`, `http://localhost:3000`, domaine prod |
| Methods | GET, POST, PUT, PATCH, DELETE, OPTIONS |
| Headers | `*` |
| Credentials | `true` |
| Max-Age | 3600 |

En production : **liste blanche stricte** (pas de `*`).

---

## 6. Rate Limiting

Filtre `RateLimitFilter` sur :

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

| Paramètre | Défaut |
|-----------|--------|
| Fenêtre | 60 s |
| Max requêtes / IP | 30 |
| Réponse | **429** `RATE_LIMIT_EXCEEDED` |

Implémentation mémoire (Caffeine / fenêtre glissante) — adaptée mono-instance ; pour scale-out, prévoir Redis.

---

## 7. Protection brute-force

`LoginAttemptService` :

| Paramètre | Défaut |
|-----------|--------|
| Échecs max | 5 |
| Durée de lock | 300 s |
| Réponse | **429** `ACCOUNT_TEMPORARILY_LOCKED` |

Complète le rate-limit IP par un verrouillage logique compte.

---

## 8. Surfaces publiques vs protégées

**Public :**

- Auth login / refresh / logout
- `/actuator/health`, `/actuator/info`
- Swagger (profils non-prod)
- Service AI endpoints (à isoler réseau)

**Protégé ADMIN+ :**

- `/actuator/prometheus`

**Authentifié + RBAC méthode :**

- Tout le reste de `/api/v1/**`

---

## 9. Best Practices

1. Ne jamais committer `.env` / secrets JWT / mots de passe DB  
2. Utiliser HTTPS + HSTS en production  
3. Ne pas exposer les ports 8080 / 8000 / 3306 sur Internet  
4. Changer le compte seed immédiatement  
5. Rotations JWT_SECRET planifiées (invalidation sessions)  
6. Limiter CORS aux origines légitimes  
7. Surveiller 401/429 dans les logs  
8. Désactiver Swagger en `prod`  
9. Backup chiffré du volume MySQL  
10. Prévoir authentification du service AI (mTLS / JWT interne) en évolution  
11. Éviter de stocker des tokens dans des cookies non `HttpOnly` sans analyse XSS (actuellement localStorage — renforcer CSP)  
12. Appliquer le principe du moindre privilège (Analyste en lecture)

---

## 11. Hardening HA (Phase 6 · Step 20)

| Contrôle | Détail |
|----------|--------|
| JWT blacklist Redis | Logout / révocation multi-instances (`JwtBlacklistFilter`) |
| API Key optionnelle | Header `X-API-Key` sur préfixes (`flypro.security.api-key.*`) |
| Rate limiting | Auth Spring + Nginx gateway |
| Rotation secrets | Voir [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md) |
| Correlation IDs | `X-Request-ID` / `X-Correlation-ID` |

---

## 12. Références code

- `com.flypro.analytics.config.SecurityConfig`
- `com.flypro.analytics.security.jwt.*`
- `com.flypro.analytics.security.ratelimit.*`
- `com.flypro.analytics.security.bruteforce.*`
- `com.flypro.analytics.infra.security.*`
- `com.flypro.analytics.infra.redis.JwtBlacklistService`
- `flypro-frontend/nginx.conf`
- `flypro-frontend/src/api/client.ts`
- `infra/gateway/nginx-gateway.conf`
