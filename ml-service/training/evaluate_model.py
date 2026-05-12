from __future__ import annotations

import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.metrics import auc, f1_score, log_loss, precision_score, recall_score, roc_curve
from sklearn.model_selection import train_test_split


ROOT = Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "training" / "data" / "ranking_events.csv"
MODEL_PATH = ROOT / "models" / "ranking_model.joblib"
METRICS_PATH = ROOT / "models" / "metrics.json"

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
    if not MODEL_PATH.exists():
        raise FileNotFoundError("Train a model first with training/train_ranker.py.")
    if not DATA_PATH.exists():
        raise FileNotFoundError("Generate synthetic data first with training/generate_synthetic_data.py.")

    frame = pd.read_csv(DATA_PATH)
    _, x_test, _, y_test = train_test_split(
        frame[FEATURE_COLUMNS],
        frame["engaged"],
        test_size=0.2,
        random_state=42,
        stratify=frame["engaged"],
    )
    model = joblib.load(MODEL_PATH)
    probabilities = model.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)
    fpr, tpr, _ = roc_curve(y_test, probabilities)
    metrics = {
        "auc": round(float(auc(fpr, tpr)), 4),
        "precision": round(float(precision_score(y_test, predictions)), 4),
        "recall": round(float(recall_score(y_test, predictions)), 4),
        "f1": round(float(f1_score(y_test, predictions)), 4),
        "log_loss": round(float(log_loss(y_test, probabilities)), 4),
        "test_rows": int(len(x_test)),
    }
    METRICS_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
