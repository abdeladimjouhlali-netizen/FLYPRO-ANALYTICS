# Documentation Intelligence Artificielle – FlyPro Analytics

**Service :** `flypro-ai` (FastAPI)  
**Algorithme principal :** Isolation Forest (Scikit-Learn)  
**Version applicative :** 1.0.0

Diagramme : [UML/ai-prediction.puml](UML/ai-prediction.puml)

---

## 1. Objectif

Détecter des **anomalies d’accès badge** (horaires inhabituels, refus répétés, zones inattendues, badges invalides, etc.) en combinant :

1. Un modèle non supervisé **Isolation Forest**
2. Un **moteur de règles métier** explicables

Le service est **stateless côté DB** : les événements sont fournis via REST ; les modèles sont persistés en fichiers Joblib.

---

## 2. Isolation Forest

Fichier : `app/ml/isolation_forest_detector.py`

| Paramètre | Défaut | Rôle |
|-----------|--------|------|
| `contamination` | 0.05 | Proportion attendue d’anomalies |
| `n_estimators` | 200 | Nombre d’arbres |
| `random_state` | 42 | Reproductibilité |
| `n_jobs` | -1 | Parallélisme |

Convention Scikit-Learn :

- Prédiction **1** = normal  
- Prédiction **-1** = anomalie  
- `decision_function` : score brut (plus élevé ≈ plus normal)

Interface abstraite `AnomalyDetector` pour permettre d’autres algorithmes futurs.

---

## 3. Feature Engineering

Fichier : `app/ml/features.py` — **10 features** ordonnées :

| Feature | Source |
|---------|--------|
| `hour` | Heure UTC de `eventTimestamp` |
| `day_of_week` | 0 = lundi … 6 = dimanche |
| `employee_id` | Identifiant employé |
| `badge_id` | Identifiant badge |
| `access_point_id` | Point d’accès |
| `zone_id` | Zone |
| `access_result_code` | AUTHORIZED=0 … TECHNICAL_ERROR=4 |
| `access_direction_code` | ENTRY=0, EXIT=1 |
| `denied_last_hour` | Compteur (0 si absent) |
| `accesses_today` | Compteur (0 si absent) |

### Prétraitement

1. Conversion événements → DataFrame  
2. Matrice `float64`  
3. `nan_to_num`  
4. **`StandardScaler`** (fit au training, transform à la prédiction)

Les champs optionnels (`employeeStatus`, `badgeStatus`, `isExpectedZone`, …) **ne sont pas** des features ML : ils alimentent uniquement les règles.

---

## 4. Processus d’entraînement

Endpoint : `POST /api/v1/ai/train` (et `/retrain`)

1. Vérifier `len(events) >= MIN_TRAINING_SAMPLES` (défaut **20**)
2. Résoudre `contamination` ∈ (0, 0.5)
3. `FeaturePreprocessor.fit_transform`
4. `IsolationForestDetector.fit`
5. Calcul métriques train (`anomaly_ratio`, stats de scores)
6. Version = `YYYYMMDDHHMMSS` (+ label optionnel)
7. Sauvegarde Joblib + `latest.json` via `ModelRegistry`
8. Chargement en mémoire

Réponse : `TrainResponse` (version, samples, features, durée, evaluation…).

---

## 5. Processus de prédiction

Endpoint : `POST /api/v1/ai/predict` (+ `/predict/batch`)

1. Charger le modèle si besoin  
2. Transformer les features  
3. `predict` + `decision_function`  
4. Évaluer `BusinessRulesEngine`  
5. Fusionner risk / décision / confidence / raison / action  

### Décision ANOMALY si

- `raw_label == -1` **OU**
- `rules.severity >= 0.35` **OU**
- `risk_score >= 0.65`

Batch : une passe ML vectorisée, puis règles événement par événement (max 500).

---

## 6. Model Management

Répertoire : `MODELS_DIR` (défaut `artifacts/models`, volume Docker `flypro_ai_models`)

| Élément | Format |
|---------|--------|
| Artefact | `isolation_forest_{version}.joblib` |
| Contenu | detector + état scaler + metadata |
| Pointeur | `latest.json` |

Opérations API :

- `GET /model` — infos + versions disponibles  
- `POST /model/load?version=` — charger une version  
- `POST /model/save` — forcer persistance  

Au démarrage (`lifespan`) : `try_load_latest()`.

---

## 7. Risk Score

Plage : **[0, 1]** (4 décimales)

1. `ml_risk = clip(0.5 - decision, 0, 1)`  
2. `risk = max(ml_risk, rules.severity)`  
3. Si règles actives et `ml_risk > 0.4` :  
   `risk = clip(0.6 * ml_risk + 0.4 * severity + 0.1, 0, 1)`

Interprétation opérationnelle :

| Risk | Lecture |
|------|---------|
| < 0.35 | Faible |
| 0.35–0.65 | Moyen — surveillance |
| ≥ 0.65 | Élevé — investigation |

---

## 8. Confidence Score

Plage : **[50.0, 99.9]**

```text
base = 55 + min(|decision|, 0.5) * 80
si ANOMALY : max(base, 50 + risk * 50)
sinon      : max(base, 50 + (1 - risk) * 45)
clip → [50, 99.9]
```

Indique la **confiance relative** du système dans sa décision (pas une probabilité bayésienne calibrée).

---

## 9. Règles métier (explicabilité)

| Flag | Condition | Severity |
|------|-----------|----------|
| OUTSIDE_WORKING_HOURS | heure ∉ [8, 18) | +0.25 |
| REPEATED_DENIED_ACCESSES | denied ≥ 3 | +0.35 |
| DENIED_ACCESS | denied ≥ 1 et résultat DENIED | +0.15 |
| UNUSUAL_FREQUENCY | accesses_today ≥ 30 | +0.20 |
| UNEXPECTED_ZONE | `isExpectedZone == false` | +0.30 |
| IMPOSSIBLE_TRAVEL | changement zone < 5 min | +0.40 |
| INACTIVE_EMPLOYEE | INACTIVE/SUSPENDED/ARCHIVED | +0.45 |
| REVOKED_BADGE | badge révoqué | +0.50 |
| EXPIRED_BADGE | badge expiré | +0.45 |
| INVALID_BADGE_STATUS | autre statut invalide | +0.40 |

**Actions recommandées :** `NONE` → `MONITOR` → `REVIEW` → `INVESTIGATE` → `BLOCK`.

---

## 10. Intégration dans la plateforme

| Chemin | Usage |
|--------|--------|
| UI `/analytics` | Appel direct `aiApi` (health + predict) |
| Backend `AI_SERVICE_URL` | URL configurée ; client HTTP métier à étendre |
| Table `anomaly_scores` | Prête pour persister les scores côté Spring |

Nginx mappe `/ai-api/` → conteneur AI.

---

## 11. Future AI Improvements

1. Authentification du service AI (JWT interne / mTLS)  
2. Pipeline d’entraînement planifié (cron / job Spring)  
3. Persistance systématique dans `anomaly_scores` + alertes `AlertTriggerSource.AI`  
4. Explainability avancée (SHAP / feature importance)  
5. Calibration de la confidence  
6. Modèles alternatifs (Autoencoder, One-Class SVM) via `AnomalyDetector`  
7. Détection drift + retrain automatique  
8. Features enrichies (graphe zones, embeddings employés)  
9. Monitoring Prometheus des latences `/predict`  
10. A/B testing de versions de modèles  

---

## 12. Dépendances clés

`fastapi`, `uvicorn`, `scikit-learn`, `pandas`, `numpy`, `joblib`, `pydantic-settings`

Voir `flypro-ai/requirements.txt`.
