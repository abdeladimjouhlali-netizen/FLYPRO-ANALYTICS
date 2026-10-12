# Monitoring

Observability for FlyPro Analytics (Compose notes + Kubernetes Step 18).

## Kubernetes (preferred)

See:

- `k8s/prometheus.yml` — scrape jobs (backend Actuator, AI, node-exporter)
- `k8s/grafana-dashboard.json` — production overview dashboard
- `k8s/monitoring-stack.yaml` — Prometheus + Grafana + Node Exporter
- `docs/MONITORING.md` — full guide

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/grafana-dashboards-configmap.yaml
kubectl apply -f k8s/monitoring-stack.yaml
```

## Spring Boot

Exposed (authenticated where required):

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus` (roles `ADMIN`, `SUPER_ADMIN`)

Micrometer Prometheus registry is on the classpath.

## Compose example

Use `prometheus.yml.example` for a local Prometheus against Docker Compose services. Do **not** expose `/actuator/prometheus` through the public Nginx gateway.
