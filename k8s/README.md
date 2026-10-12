# FlyPro Analytics – Kubernetes (Phase 5 · Step 18)

Cloud-ready manifests for production deployment. **No business logic changes.**

## Quick start

```bash
# Edit secrets first (CHANGE_ME*)
kubectl apply -f namespace.yaml
kubectl apply -f secret.yaml
kubectl apply -k .
kubectl apply -f grafana-dashboards-configmap.yaml
kubectl apply -f monitoring-stack.yaml
kubectl apply -f logging.yaml
kubectl apply -f tracing.yaml
```

## Deliverables map

| File | Purpose |
|------|---------|
| `frontend-deployment.yaml` | SPA + Nginx gateway |
| `backend-deployment.yaml` | Spring Boot API |
| `ai-deployment.yaml` | FastAPI AI |
| `postgres-deployment.yaml` | **MySQL 8.4** StatefulSet (app-compatible) |
| `services.yaml` | ClusterIP services |
| `ingress.yaml` | TLS-ready Ingress |
| `configmap.yaml` | Non-secret config + Nginx |
| `secret.yaml` | Credentials placeholders |
| `hpa.yaml` | Horizontal Pod Autoscaler |
| `namespace.yaml` | `flypro` + `flypro-observability` |
| `prometheus.yml` | Scrape configuration |
| `grafana-dashboard.json` | Grafana overview dashboard |
| `backup.sh` / `restore.sh` | DB backup & restore |

Additional: `network-policy.yaml`, `backup-cronjob.yaml`, `monitoring-stack.yaml`, `logging.yaml`, `tracing.yaml`, `kustomization.yaml`.

## Database note

Step 18 text cites PostgreSQL. The delivered Spring Boot stack uses **MySQL**. Manifests deploy MySQL so the cluster is **actually runnable** without schema/API changes.

## Docs

- [Kubernetes Guide](../docs/KUBERNETES.md)
- [Monitoring](../docs/MONITORING.md)
- [Logging](../docs/LOGGING.md)
- [Scaling](../docs/SCALING.md)
- [Backup](../docs/BACKUP.md)
- [Recovery](../docs/RECOVERY.md)
