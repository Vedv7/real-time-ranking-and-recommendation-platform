# System Design

## Goals

- Serve personalized feeds with low latency.
- Decouple event ingestion from feature updates.
- Keep features durable in PostgreSQL and fast in Redis.
- Support ML serving with graceful local fallback.
- Run ranking experiments and persist policy/model metadata for analysis.
- Log ranking decisions for debugging and offline analysis.

## Candidate Generation

Candidates come from three sources:

- Recent content from the configured time window.
- Popular content by content feature score.
- Content from categories preferred by the user.

Previously interacted content is filtered out before ranking.

## Ranking

The ranking service builds feature vectors for up to 100 candidates, calls the ML inference service, assigns the user to an experiment bucket, applies a ranking policy, sorts candidates, and returns the top `N`.

Baseline score:

```text
final_score = 0.65 * predicted_ctr
            + 0.15 * freshness_score
            + 0.10 * popularity_score
            + 0.10 * category_match
```

Ranking policies:

- `CONTROL`: baseline ranking.
- `FRESHNESS_BOOST`: adds extra freshness weight.
- `DIVERSITY_BOOST`: penalizes overrepresented categories.
- `EXPLORATION_BOOST`: boosts novel categories to discover new user interests.

## Feature Store Diagnostics

Online feature tables track `updatedAt` timestamps. `/api/platform/feature-store` reports user/content feature counts and stale feature counts against the freshness threshold.

## Model Metadata

The ML service returns model version, stage, loaded timestamp, and fallback status. The backend copies this metadata into feed responses and recommendation logs.

## Caching

Redis cache keys:

- `user-feature:{userId}`
- `content-feature:{contentId}`
- `feed:{userId}:{limit}`

Event consumption invalidates feed cache entries for the affected user.

## Reliability Notes

Kafka, Redis, and ML calls are wired through real clients, while Redis and ML failures degrade gracefully so local demos remain stable. Logs include enough metadata to replay ranking decisions and compare policy/model versions offline.
