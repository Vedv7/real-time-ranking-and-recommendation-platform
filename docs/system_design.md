# System Design

## Goals

- Serve personalized feeds with low latency.
- Decouple event ingestion from feature updates.
- Keep features durable in PostgreSQL and fast in Redis.
- Support ML serving with graceful local fallback.
- Log ranking decisions for debugging and offline analysis.

## Candidate Generation

Candidates come from three sources:

- Recent content from the configured time window.
- Popular content by content feature score.
- Content from categories preferred by the user.

Previously interacted content is filtered out before ranking.

## Ranking

The ranking service builds feature vectors for up to 100 candidates, calls the ML inference service, calculates a blended score, sorts candidates, and returns the top `N`.

## Caching

Redis cache keys:

- `user-feature:{userId}`
- `content-feature:{contentId}`
- `feed:{userId}:{limit}`

Event consumption invalidates feed cache entries for the affected user.

## Reliability Notes

The first local version favors clear behavior over distributed complexity. Kafka, Redis, and ML calls are wired through real clients, while Redis and ML failures degrade gracefully so local demos remain stable.
