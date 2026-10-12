# Logging Guide – FlyPro Analytics

---

## 1. Objectif

Centraliser les logs applicatifs avec corrélation :

| Champ | Description |
|-------|-------------|
| `timestamp` | ISO-8601 |
| `service` | backend / frontend / ai |
| `request_id` | ID de corrélation (Nginx `X-Request-ID`) |
| `user` | Identifiant si présent dans le log métier |
| `level` | INFO / WARN / ERROR |

---

## 2. Architecture

```text
Pods (stdout) → Fluent Bit DaemonSet → OpenSearch (index flypro-*)
                                      ↘ (compatible Elasticsearch / ELK)
```

Manifest : `k8s/logging.yaml`

- **Fluent Bit** : tail des logs containers du namespace `flypro`
- **OpenSearch** : single-node (démo) — remplacer par OpenSearch managé / ELK en prod
- Enrichissement Kubernetes (pod, namespace, labels)

---

## 3. Format recommandé (JSON)

Exemple backend / AI :

```json
{
  "timestamp": "2026-07-27T16:00:00Z",
  "service": "flypro-backend",
  "request_id": "abc-123",
  "user": "admin",
  "level": "ERROR",
  "message": "Access denied"
}
```

Le ConfigMap Nginx (`flypro-frontend-config`) émet déjà un `access_log` JSON avec `request_id`, `service`, `timestamp`, `level`.

Propagation : header `X-Request-ID` ajouté sur les proxies `/api` et `/ai-api`.

---

## 4. Déploiement

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/logging.yaml
```

Sécurité OpenSearch démo désactivée (`plugins.security.disabled=true`) — **interdit en production publique**. Utiliser TLS + auth.

---

## 5. Alternative ELK

Pointer Fluent Bit `OUTPUT` vers Elasticsearch :

```text
Host  elasticsearch.logging.svc
Port  9200
Index flypro-logs
```

Kibana / OpenSearch Dashboards pour les recherches `request_id:"..."`.

---

## 6. Rétention & ops

- ILM / ISM policies (7–30 jours selon conformité)
- Ne pas logger de secrets / tokens JWT
- Alerter sur taux d’ERROR backend
