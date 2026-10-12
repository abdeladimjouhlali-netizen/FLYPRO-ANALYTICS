# Guide de maintenance – FlyPro Analytics

**Public :** développeurs et mainteneurs  
**Version :** 1.0.0

---

## 1. Structure du projet

```text
pfe-Ram/
├── docker-compose.yml          # Stack de livraison
├── .env.example
├── .github/workflows/ci.yml
├── docs/                       # Documentation PFE (ce dossier)
├── scripts/                    # deploy, smoke, wait-healthy
├── monitoring/                 # Notes Prometheus
├── flypro-backend/             # Spring Boot (implémentation)
├── flypro-frontend/            # React SPA
├── flypro-ai/                  # FastAPI Isolation Forest
└── flypro-analytics/           # Squelette historique (non livraison)
```

**Source de vérité runtime :** services racine `flypro-*` + Compose racine.

---

## 2. Structure des dossiers applicatifs

### Backend (`flypro-backend/src/main/java/com/flypro/analytics`)

```text
<module>/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
  specification/
  exception/
config/  security/  common/
resources/db/migration/   # Flyway
```

### Frontend (`flypro-frontend/src`)

```text
features/<domaine>/   # pages
api/                  # client + services
components/ contexts/ routes/ layouts/ types/
```

### AI (`flypro-ai/app`)

```text
api/routers/  ml/  services/  schemas/  core/
artifacts/models/
```

---

## 3. Coding standards

| Zone | Standard |
|------|----------|
| Java | Java 21, Spring Boot 3.5, MapStruct, constructeur injection |
| API | Préfixe `/api/v1`, enveloppe `ApiResponse`, DTOs validés |
| Sécurité | `@PreAuthorize` sur écritures sensibles |
| DB | Flyway only — **pas** de `ddl-auto=update` en prod |
| Frontend | TypeScript strict, feature folders, TanStack Query keys stables |
| AI | Pydantic camelCase, tests pytest, modèles versionnés |
| Git | Pas de secrets ; CI verte avant merge |

---

## 4. Ajouter un nouveau module métier (backend)

1. Créer le package `com.flypro.analytics.<module>`
2. Entity (+ migration Flyway `V11__...sql`)
3. Repository + Specification si filtres
4. DTOs + Mapper MapStruct
5. Service (règles métier)
6. Controller REST + `@PreAuthorize`
7. Tests (service / controller)
8. Exposer le client dans `flypro-frontend/src/api/services.ts`
9. Ajouter `features/<module>` + route + entrée `NAV_ITEMS` si besoin
10. Documenter dans `docs/API.md` et `docs/DATABASE.md`

---

## 5. Créer une nouvelle API

1. Définir le contrat (méthode, path, body, rôles, codes erreur)
2. Ajouter la méthode dans le `*Controller`
3. Implémenter le service
4. Mettre à jour OpenAPI (annotations Springdoc si utilisées)
5. Ajouter le client frontend + page / mutation React Query
6. Couvrir par un test d’intégration si critique
7. Mettre à jour [API.md](API.md)

Exemple de signature :

```java
@PostMapping
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public ResponseEntity<ApiResponse<MyResponse>> create(@Valid @RequestBody MyRequest req) { ... }
```

---

## 6. Ajouter des tables base de données

1. Créer `src/main/resources/db/migration/Vxx__description.sql`
2. Définir PK, FK, UK, indexes
3. Créer l’entité JPA alignée (`ddl-auto=validate`)
4. Tester sur une DB vide (`docker compose down -v && up`)
5. Documenter l’entité dans [DATABASE.md](DATABASE.md)
6. Mettre à jour [UML/erd.puml](UML/erd.puml)

**Interdit :** modifier une migration déjà appliquée en production — créer une nouvelle version.

---

## 7. Étendre l’IA

| Objectif | Action |
|----------|--------|
| Nouvelle feature ML | Étendre `features.py` + réentraîner |
| Nouvelle règle | Ajouter dans `rules_engine.py` + tests |
| Nouvel algorithme | Implémenter `AnomalyDetector` + registry |
| Persistance scores | Brancher backend → AI + table `anomaly_scores` |
| Alerte auto IA | Règle `AlertTriggerSource.AI` côté Spring |

Toujours versionner le modèle et conserver les artefacts sur le volume Docker.

---

## 8. Opérations courantes

```bash
# Logs
docker compose logs -f backend
docker compose logs -f ai-service

# Rebuild un service
docker compose up --build -d backend

# Backup MySQL (exemple)
docker exec flypro-mysql mysqldump -u root -p flypro_analytics > backup.sql

# Smoke
./scripts/smoke-test.sh
```

---

## 9. Troubleshooting

| Problème | Piste |
|----------|-------|
| Flyway checksum mismatch | Ne pas éditer une V déjà jouée ; réparation manuelle avancée uniquement |
| 403 inattendu | Vérifier rôle JWT vs `@PreAuthorize` |
| CORS | `CORS_ALLOWED_ORIGINS` vs URL réelle du front |
| AI model missing | Appeler `/train` ou vérifier volume `flypro_ai_models` |
| Frontend API 404 | Nginx `/api/` vs `VITE_API_BASE_URL` |

---

## 10. Qualité & dette

Voir [QUALITY_REPORT.md](QUALITY_REPORT.md) (Step 16) pour l’état sécurité / perf / dette technique connue.
