# Resume Bullets

- Built a production-style real-time recommendation platform with Java 17, Spring Boot 3, Kafka, Redis, PostgreSQL, Docker Compose, and FastAPI ML serving.
- Implemented event-driven interaction ingestion for views, likes, clicks, skips, saves, shares, and watch time, with Kafka consumers updating user interest, content popularity, freshness, and engagement features.
- Designed candidate generation and ranking services that exclude seen content, combine recent/popular/preference-matched candidates, call an ML CTR inference service, and return explainable feed-ranking metadata.
- Trained an XGBoost binary classifier on 250,000 synthetic interaction events with AUC, precision, recall, F1, and log-loss reporting.
- Added Redis feature/feed caching, recommendation decision logging, Swagger API docs, actuator metrics, local seed scripts, and Docker Compose infrastructure for reproducible demos.
