from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "training" / "data"
DATA_DIR.mkdir(parents=True, exist_ok=True)


def sigmoid(values: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-values))


def main() -> None:
    rng = np.random.default_rng(42)
    n_users = 10_000
    n_content = 5_000
    n_events = 250_000

    users = pd.DataFrame(
        {
            "user_id": np.arange(1, n_users + 1),
            "user_total_interactions": rng.poisson(120, n_users).clip(0, 800),
            "user_avg_watch_time": rng.gamma(5.0, 3.5, n_users).clip(1, 120),
            "user_like_rate": rng.beta(2.2, 8.0, n_users),
            "user_skip_rate": rng.beta(2.0, 10.0, n_users),
        }
    )

    content = pd.DataFrame(
        {
            "content_id": np.arange(1, n_content + 1),
            "content_popularity_score": rng.beta(2.0, 5.0, n_content),
            "content_freshness_score": rng.beta(3.0, 2.5, n_content),
            "content_engagement_rate": rng.beta(2.0, 6.0, n_content),
            "content_age_hours": rng.exponential(48, n_content).clip(0, 720),
        }
    )

    event_users = users.iloc[rng.integers(0, n_users, n_events)].reset_index(drop=True)
    event_content = content.iloc[rng.integers(0, n_content, n_events)].reset_index(drop=True)
    category_match = rng.binomial(1, 0.34, n_events)
    creator_affinity = rng.beta(1.4, 8.0, n_events)

    frame = pd.concat([event_users.drop(columns=["user_id"]), event_content.drop(columns=["content_id"])], axis=1)
    frame["category_match"] = category_match
    frame["creator_affinity"] = creator_affinity

    logits = (
        -1.2
        + 1.35 * frame["category_match"]
        + 1.15 * frame["content_engagement_rate"]
        + 0.95 * frame["content_popularity_score"]
        + 0.80 * frame["content_freshness_score"]
        + 0.55 * frame["creator_affinity"]
        + 0.50 * frame["user_like_rate"]
        - 1.10 * frame["user_skip_rate"]
        - 0.012 * frame["content_age_hours"].clip(upper=168)
        + rng.normal(0, 0.45, n_events)
    )
    frame["engaged"] = rng.binomial(1, sigmoid(logits))
    output_path = DATA_DIR / "ranking_events.csv"
    frame.to_csv(output_path, index=False)

    print(f"wrote {len(frame):,} events to {output_path}")


if __name__ == "__main__":
    main()
