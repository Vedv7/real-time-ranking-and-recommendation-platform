from __future__ import annotations

import math

import pandas as pd

from app.schemas import RankingFeatures


FEATURE_COLUMNS = [
    "user_total_interactions",
    "user_avg_watch_time",
    "user_like_rate",
    "user_skip_rate",
    "content_popularity_score",
    "content_freshness_score",
    "content_engagement_rate",
    "category_match",
    "creator_affinity",
    "content_age_hours",
]


def to_frame(features: list[RankingFeatures]) -> pd.DataFrame:
    return pd.DataFrame([feature.model_dump() for feature in features], columns=FEATURE_COLUMNS)


def deterministic_score(feature: RankingFeatures) -> float:
    log_interactions = min(1.0, math.log1p(feature.user_total_interactions) / 8.0)
    raw = (
        -0.8
        + 1.4 * feature.category_match
        + 1.1 * feature.content_engagement_rate
        + 0.9 * feature.content_popularity_score
        + 0.7 * feature.content_freshness_score
        + 0.6 * feature.creator_affinity
        + 0.4 * feature.user_like_rate
        + 0.2 * log_interactions
        - 1.0 * feature.user_skip_rate
        - 0.015 * min(feature.content_age_hours, 168)
    )
    return 1.0 / (1.0 + math.exp(-raw))


def confidence_from_probability(probability: float) -> float:
    return round(0.55 + abs(probability - 0.5) * 0.8, 4)
