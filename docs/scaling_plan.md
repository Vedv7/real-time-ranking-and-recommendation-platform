# Scaling Plan

This project runs locally, but the architecture maps to a production feed ranking system.

## 10K Users

- Single Spring Boot service behind a load balancer.
- PostgreSQL primary with daily backups.
- Redis for feature and feed cache.
- Kafka with three partitions for interaction events.

## 1M Users

- Split API, event ingestion, feature computation, and ranking into separate deployable services.
- Use Kafka partitions keyed by `userId`.
- Move feature aggregation to Kafka Streams, Flink, or Spark Structured Streaming.
- Add read replicas for analytics queries and recommendation logs.
- Store offline features in a warehouse such as BigQuery, Snowflake, Redshift, or Iceberg tables.

## 100M Users

- Dedicated candidate retrieval service backed by vector search and inverted indexes.
- Online feature store with strict freshness SLOs.
- Model registry, shadow deployments, canary rollout, and policy versioning.
- Multi-region cache and feed precomputation for high-traffic users.
- Real-time monitoring for latency, stale features, model drift, and engagement regressions.

## Failure Modes

- ML service unavailable: backend falls back to deterministic scoring.
- Redis unavailable: backend recomputes feed and continues serving.
- Kafka delayed: durable events remain in PostgreSQL, but online features become stale.
- Model quality regression: ranking logs and offline backtests allow rollback by model version.
