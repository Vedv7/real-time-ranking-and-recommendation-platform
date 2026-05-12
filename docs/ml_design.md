# ML Design

The model predicts whether a user is likely to click or positively engage with a content item. The output is treated as predicted CTR and blended with freshness, popularity, and category-match signals.

## Features

- `user_total_interactions`
- `user_avg_watch_time`
- `user_like_rate`
- `user_skip_rate`
- `content_popularity_score`
- `content_freshness_score`
- `content_engagement_rate`
- `category_match`
- `creator_affinity`
- `content_age_hours`

## Training

`training/generate_synthetic_data.py` creates 10,000 users, 5,000 content items, and 250,000 interaction examples. `training/train_ranker.py` trains an `XGBClassifier` and saves:

- `models/ranking_model.joblib`
- `models/metrics.json`
- `models/model_metadata.json`
- `models/feature_importance.png`

Metrics include AUC, precision, recall, F1, and log loss.

## Serving

## Offline Policy Backtesting

`training/backtest_policies.py` compares `control`, `freshness_boost`, `diversity_boost`, and `exploration_boost` ranking policies. It reports:

- `precision_at_10`
- `ndcg_at_10`
- `high_skip_exposure_rate`
- `evaluated_users`

## Serving

FastAPI exposes `POST /predict` and `GET /metadata`. If no model artifact exists, the service uses deterministic scoring so the system can run immediately after cloning. This fallback is intentionally visible through `GET /health`, `/metadata`, and prediction responses.
