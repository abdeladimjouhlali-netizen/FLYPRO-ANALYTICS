# Monitoring Guide – FlyPro Analytics

---

## 1. Stack

| Composant | Rôle |
|-----------|------|
| **Prometheus** | Collecte métriques |
| **Grafana** | Dashboards |
| **Spring Boot Actuator + Micrometer** | JVM, HTTP, HikariCP |
| **Node Exporter** | CPU / mémoire nœuds |
| **mysqld-exporter** (optionnel) | Métriques MySQL |
| **FastAPI** | Health scrape ; `/metrics` quand instrumenté |

Manifests : `k8s/monitoring-stack.yaml`, scrape : `k8s/prometheus.yml`.

---

## 2. Spring Boot / Micrometer

Endpoints (réseau interne) :

- `/actuator/health`, `/info`
- `/actuator/prometheus` (souvent ADMIN+)
- `/actuator/metrics`

Métriques utiles :

- `http_server_requests_seconds_*` — latence & débit API
- `jvm_memory_used_bytes`, `jvm_threads_*`
- `hikaricp_connections_*`
- `process_cpu_usage`

Si Prometheus reçoit 401 : fournir un Bearer ADMIN via `authorization` dans `prometheus.yml`.

---

## 3. FastAPI metrics

Le service expose `/api/v1/ai/health` (scrape uptime).  
Pour des métriques de prédiction (latency, ANOMALY count), activer un instrumentator Prometheus **sans changer la logique métier** (dépendance + middleware). Le dashboard prévoit les séries `flypro_ai_*` / `http_request_duration_seconds_*`.

---

## 4. Grafana dashboards

Fichier : `k8s/grafana-dashboard.json` — **FlyPro Analytics – Production Overview**

Panneaux :

- API request rate & response time p95  
- Request count  
- JVM heap / threads  
- CPU & memory containers  
- HikariCP / MySQL exporter  
- AI health / prediction placeholders  
- HPA desired replicas  

Import :

```bash
# UI Grafana → Dashboards → Import → Upload JSON
# ou ConfigMap
kubectl -n flypro-observability create configmap grafana-dashboards \
  --from-file=flypro-overview.json=k8s/grafana-dashboard.json \
  -o yaml --dry-run=client | kubectl apply -f -
```

Datasource Prometheus UID attendu : `prometheus` (renommer ou adapter le JSON).

---

## 5. Accès

```bash
kubectl -n flypro-observability port-forward svc/prometheus 9090:9090
kubectl -n flypro-observability port-forward svc/grafana 3000:3000
```

Changer `GF_SECURITY_ADMIN_PASSWORD` dans `monitoring-stack.yaml`.

---

## 6. Alerting (prochaines étapes)

- Alertmanager + règles sur `http_server_requests` 5xx  
- Alerte pod not ready / HPA maxed  
- Disque PVC MySQL > 80 %  
