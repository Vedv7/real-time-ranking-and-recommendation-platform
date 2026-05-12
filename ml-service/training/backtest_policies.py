from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.metrics import ndcg_score


ROOT = Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "training" / "data" / "ranking_events.csv"
OUTPUT_PATH = ROOT / "models" / "backtest_report.json"


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


def main() -> None:
    if not DATA_PATH.exists():
        raise FileNotFoundError("Run training/generate_synthetic_data.py before backtesting.")

    frame = pd.read_csv(DATA_PATH)
    if "user_id" not in frame.columns:
        raise ValueError("Synthetic data must include user_id. Regenerate data with generate_synthetic_data.py.")

    sampled = frame.groupby("user_id", group_keys=False).sample(n=20, replace=True, random_state=42)
    policies = {
        "control": control_score,
        "freshness_boost": freshness_boost_score,
        "diversity_boost": diversity_boost_score,
        "exploration_boost": exploration_boost_score,
    }

    report = {}
    for name, scorer in policies.items():
        scored = sampled.copy()
        scored["policy_score"] = scorer(scored)
        report[name] = evaluate_policy(scored)

    OUTPUT_PATH.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))


def control_score(frame: pd.DataFrame) -> pd.Series:
    return (
        0.65 * synthetic_ctr(frame)
        + 0.15 * frame["content_freshness_score"]
        + 0.10 * frame["content_popularity_score"]
        + 0.10 * frame["category_match"]
    )


def freshness_boost_score(frame: pd.DataFrame) -> pd.Series:
    return control_score(frame) + 0.10 * frame["content_freshness_score"]


def diversity_boost_score(frame: pd.DataFrame) -> pd.Series:
    category_density = frame.groupby(["user_id", "category_match"])["category_match"].transform("count") / 20.0
    return control_score(frame) - 0.08 * category_density


def exploration_boost_score(frame: pd.DataFrame) -> pd.Series:
    novelty = 1 - frame["category_match"]
    return control_score(frame) + 0.08 * (0.6 * novelty + 0.4 * frame["content_freshness_score"])


def synthetic_ctr(frame: pd.DataFrame) -> pd.Series:
    logits = (
        -0.8
        + 1.4 * frame["category_match"]
        + 1.1 * frame["content_engagement_rate"]
        + 0.9 * frame["content_popularity_score"]
        + 0.7 * frame["content_freshness_score"]
        + 0.6 * frame["creator_affinity"]
        + 0.4 * frame["user_like_rate"]
        - 1.0 * frame["user_skip_rate"]
        - 0.015 * frame["content_age_hours"].clip(upper=168)
    )
    return 1.0 / (1.0 + np.exp(-logits))


def evaluate_policy(frame: pd.DataFrame) -> dict[str, float]:
    precision_values = []
    ndcg_values = []
    skip_rate_values = []

    for _, group in frame.groupby("user_id"):
        ranked = group.sort_values("policy_score", ascending=False).head(10)
        precision_values.append(float(ranked["engaged"].mean()))
        skip_rate_values.append(float((ranked["user_skip_rate"] > 0.25).mean()))
        if group["engaged"].nunique() > 1:
            ndcg_values.append(float(ndcg_score([group["engaged"].to_numpy()], [group["policy_score"].to_numpy()], k=10)))

    return {
        "precision_at_10": round(float(np.mean(precision_values)), 4),
        "ndcg_at_10": round(float(np.mean(ndcg_values)), 4) if ndcg_values else 0.0,
        "high_skip_exposure_rate": round(float(np.mean(skip_rate_values)), 4),
        "evaluated_users": int(frame["user_id"].nunique()),
    }


if __name__ == "__main__":
    main()
