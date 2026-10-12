# Scaling Guide – FlyPro Analytics

---

## 1. Principes

| Couche | Stratégie |
|--------|-----------|
| Frontend | Stateless – scale horizontal libre |
| Backend | Stateless (JWT) – scale horizontal ; DB partagée |
| AI | Stateless (modèles sur PVC) – attention RWO PVC |
| MySQL | Vertical + backups ; HA via opérateur managé (Cloud SQL / RDS / Azure DB) |

---

## 2. HPA

Fichier : `k8s/hpa.yaml`

| Target | Min | Max | CPU | Memory |
|--------|-----|-----|-----|--------|
| backend | 2 | 8 | 70 % | 80 % |
| ai-service | 2 | 6 | 65 % | 75 % |
| frontend | 2 | 6 | 70 % | — |

Prérequis : **metrics-server** installé.

```bash
kubectl get hpa -n flypro
kubectl describe hpa backend-hpa -n flypro
```

---

## 3. Resources requests / limits

Définis dans chaque Deployment :

| Service | Request CPU/Mem | Limit CPU/Mem |
|---------|-----------------|---------------|
| backend | 250m / 512Mi | 1000m / 1536Mi |
| ai | 200m / 512Mi | 1000m / 2Gi |
| frontend | 50m / 64Mi | 250m / 256Mi |
| mysql | 250m / 512Mi | 1000m / 2Gi |

Ajuster selon charge réelle (Grafana JVM / CPU).

---

## 4. Rolling updates

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

Zéro downtime applicatif tant que readiness passe.

```bash
kubectl -n flypro rollout status deployment/backend
kubectl -n flypro rollout undo deployment/backend   # rollback
```

---

## 5. PVC & AI scaling

`ai-models-pvc` est **ReadWriteOnce** : multi-replicas sur nœuds différents peut bloquer.

Options :

1. Répliquer artefacts dans chaque pod (initContainer sync objet storage)
2. Storage **ReadWriteMany** (EFS / Azure Files / NFS)
3. Garder `replicas: 1` pour AI jusqu’à stockage partagé

---

## 6. Base de données

Ne pas scaler MySQL StatefulSet à N sans réplication.

Prod recommandée :

- Service managé (RDS / Cloud SQL / Flexible Server)
- Connection pool (`DB_POOL_SIZE`) adapté au max pods × connexions

---

## 7. Tests de charge

1. Baseline Grafana (p95 API, CPU)  
2. Augmenter QPS (k6 / JMeter)  
3. Vérifier HPA `desiredReplicas`  
4. Surveiller erreurs 5xx / latence DB  
