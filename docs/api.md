# API

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html` when the backend is running.

## Users

- `POST /api/users`
- `GET /api/users/{id}`

## Content

- `POST /api/content`
- `GET /api/content`
- `GET /api/content/{id}`

## Events

- `POST /api/events`
- `GET /api/events/user/{userId}`

Supported event types:

- `VIEW`
- `LIKE`
- `CLICK`
- `SKIP`
- `SAVE`
- `SHARE`
- `WATCH_TIME`

## Feed

- `GET /api/feed/{userId}?limit=20`

The feed response includes request latency, candidate count, experiment bucket, ranking policy, model version, ranked items, predicted CTR, popularity score, user-interest score, freshness score, exploration score, diversity penalty, final score, rank position, and explanations.

## Platform Diagnostics

- `GET /api/platform/feature-store`
- `GET /api/platform/model`
- `GET /api/platform/stats`
- `GET /api/platform/model-registry`
- `GET /api/platform/experiments`
- `GET /api/platform/experiments/results`
- `GET /api/platform/recommendation-logs/{userId}`
- `GET /api/platform/feature-timeline/{userId}`
- `POST /api/platform/demo/reset`
- `POST /api/platform/replay`

These endpoints expose online feature freshness, active ML model metadata, model registry entries, ranking experiment definitions, experiment analytics, persisted ranking decisions, feature-change timelines, replay simulation, and a repeatable demo reset workflow.

`POST /api/platform/demo/reset` clears local runtime data, flushes Redis when available, and creates a demo user with varied ML/backend-heavy content and starter feature snapshots. It is intended for local demos, not production traffic.

Set `DEMO_RESET_ENABLED=false` to disable the reset endpoint in shared or deployed environments.

`POST /api/platform/replay` accepts a `userId`, `eventCount`, and `focusCategory`, then synchronously replays synthetic behavior into the feature store. This is useful for local ranking demos and backtesting-style walkthroughs.

## ML Service

- `GET /health`
- `GET /metadata`
- `POST /predict`

`POST /predict` accepts a list of ranking feature vectors and returns predicted CTR probabilities plus model metadata.
