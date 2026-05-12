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

The feed response includes request latency, ranked items, predicted CTR, popularity score, user-interest score, freshness score, final score, rank position, and explanations.

## ML Service

- `GET /health`
- `POST /predict`

`POST /predict` accepts a list of ranking feature vectors and returns predicted CTR probabilities.
