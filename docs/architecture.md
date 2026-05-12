# Architecture

This project models a real-time personalized feed system with a Spring Boot recommendation backend, Kafka event ingestion, PostgreSQL durability, Redis online caches, and a FastAPI ML inference service.

## Request Flow

1. `POST /api/events` validates the user/content pair, persists an `InteractionEvent`, and publishes the event to Kafka topic `user-interaction-events`.
2. `EventConsumerService` consumes events and calls `FeatureUpdateService`.
3. `FeatureUpdateService` updates user preferences, user engagement rates, content counters, popularity, freshness, and Redis caches.
4. `GET /api/feed/{userId}` checks Redis feed cache, generates candidates, builds ranking features, calls ML inference, blends scores, persists recommendation logs, and returns ranked content.

## Storage

- PostgreSQL stores users, content, events, online feature snapshots, and recommendation logs.
- Redis stores low-latency user feature, content feature, and feed cache entries.
- Kafka decouples write-path event ingestion from feature updates.

## Local Deployment

`docker-compose.yml` runs PostgreSQL, Redis, Zookeeper, Kafka, the Java backend, and the Python ML service.
