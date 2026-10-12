# High Availability – FlyPro Analytics (Phase 6 · Step 20)

## Cibles

| Domaine | Mesure |
|---------|--------|
| API | ≥ 2 replicas backend + HPA |
| IA | ≥ 2 replicas ai-service + HPA |
| Données | MySQL primary (+ replica lecture optionnelle) |
| Cache | Redis (HA managé en prod) |
| Async | RabbitMQ (cluster / Quorum queues en prod) |
| Edge | Nginx / Ingress multi-nœuds |

## Stack HA locale

```bash
docker compose -f docker-compose.yml -f docker-compose.ha.yml up -d
# Replica MySQL optionnelle :
docker compose -f docker-compose.yml -f docker-compose.ha.yml --profile ha-replica up -d
```

## Base de données

- **Runtime livré : MySQL 8.4** (GTID / binlog : `infra/ha/mysql/`)
- Templates **PostgreSQL** streaming / Patroni : `infra/ha/postgresql/` (préparation future, sans migration d’entités)

## Application

- Graceful shutdown Spring
- Resilience4j vers l’IA
- Cache Redis partagé (blacklist JWT multi-pods)
- Correlation IDs + OTEL / Jaeger
- Connection pool Hikari + batch Hibernate

## Diagrammes

- [`docs/UML/ha-architecture.puml`](UML/ha-architecture.puml)
- [`docs/UML/ha-async-sequence.puml`](UML/ha-async-sequence.puml)
- [`docs/UML/ha-resilience-sequence.puml`](UML/ha-resilience-sequence.puml)

## Documents liés

- [REDIS.md](REDIS.md) · [RABBITMQ.md](RABBITMQ.md) · [RESILIENCE4J.md](RESILIENCE4J.md)
- [API_GATEWAY.md](API_GATEWAY.md) · [DISASTER_RECOVERY.md](DISASTER_RECOVERY.md)
- [SCALING.md](SCALING.md) · [RECOVERY.md](RECOVERY.md)
