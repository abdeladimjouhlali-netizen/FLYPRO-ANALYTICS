# RabbitMQ – FlyPro Analytics (Phase 6 · Step 20)

## Rôle

Broker de messages pour jobs asynchrones et découplage infra (import, IA, alertes, audit, notifications, refresh dashboard).

## Topologie (déclarée par Spring)

| Queue | Routing | DLQ |
|-------|---------|-----|
| `flypro.access-events` | `access.event.#` | `flypro.dlq` via DLX |
| `flypro.alerts` | `alert.#` | idem |
| `flypro.import-jobs` | `import.#` | idem |
| `flypro.ai-predictions` | `ai.predict.#` | idem |
| `flypro.audit-logs` | `audit.#` | idem |
| `flypro.notifications` | `notification.#` | idem |
| `flypro.dashboard-refresh` | `dashboard.#` | idem |
| `flypro.retry` | TTL 30s → requeue | — |
| `flypro.dlq` | `dlq` | terminal |

Exchange principal : topic `flypro.events`  
Dead-letter exchange : direct `flypro.dlx`

## Activation

```bash
RABBITMQ_ENABLED=true
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=flypro
RABBITMQ_PASSWORD=***
```

UI management : port `15672` (Compose HA).

## Code

- Publisher : `com.flypro.analytics.infra.messaging.AsyncJobPublisher`
- Listeners stubs : `AsyncJobListeners` (log only — **pas de câblage métier**)
- Facade async : `AsyncJobService`

Si RabbitMQ est off : publication dégradée en log (fault tolerance).

## Important

Les APIs métier restent synchrones. RabbitMQ est un **port d’infrastructure** prêt pour brancher les workers sans changer les contrats REST.
