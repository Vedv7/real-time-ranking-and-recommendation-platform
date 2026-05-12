from __future__ import annotations

import json
from pathlib import Path
from datetime import datetime, timezone

import joblib
import matplotlib.pyplot as plt
import pandas as pd
from sklearn.metrics import auc, f1_score, log_loss, precision_score, recall_score, roc_curve
from sklearn.model_selection import train_test_split
from xgboost import XGBClassifier


ROOT = Path(__file__).resolve().parents[1]
DATA_PATH = ROOT / "training" / "data" / "ranking_events.csv"
MODEL_DIR = ROOT / "models"
MODEL_DIR.mkdir(parents=True, exist_ok=True)

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
        raise FileNotFoundError("Run training/generate_synthetic_data.py before training.")

    frame = pd.read_csv(DATA_PATH)
    x_train, x_test, y_train, y_test = train_test_split(
        frame[FEATURE_COLUMNS],
        frame["engaged"],
        test_size=0.2,
        random_state=42,
        stratify=frame["engaged"],
    )

    model = XGBClassifier(
        n_estimators=250,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.9,
        colsample_bytree=0.9,
        eval_metric="logloss",
        tree_method="hist",
        random_state=42,
    )
    model.fit(x_train, y_train)

    probabilities = model.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)
    fpr, tpr, _ = roc_curve(y_test, probabilities)
    metrics = {
        "model_version": model_version(),
        "auc": round(float(auc(fpr, tpr)), 4),
        "precision": round(float(precision_score(y_test, predictions)), 4),
        "recall": round(float(recall_score(y_test, predictions)), 4),
        "f1": round(float(f1_score(y_test, predictions)), 4),
        "log_loss": round(float(log_loss(y_test, probabilities)), 4),
        "training_rows": int(len(x_train)),
        "test_rows": int(len(x_test)),
    }

    joblib.dump(model, MODEL_DIR / "ranking_model.joblib")
    (MODEL_DIR / "metrics.json").write_text(json.dumps(metrics, indent=2), encoding="utf-8")
    (MODEL_DIR / "model_metadata.json").write_text(
        json.dumps(
            {
                "model_version": metrics["model_version"],
                "model_stage": "production",
                "trained_at": datetime.now(timezone.utc).isoformat(),
                "training_rows": metrics["training_rows"],
                "test_rows": metrics["test_rows"],
                "primary_metric": "auc",
                "primary_metric_value": metrics["auc"],
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    plot_feature_importance(model)

    print(json.dumps(metrics, indent=2))


def plot_feature_importance(model: XGBClassifier) -> None:
    importance = pd.Series(model.feature_importances_, index=FEATURE_COLUMNS).sort_values()
    plt.figure(figsize=(9, 6))
    importance.plot(kind="barh", title="Ranking Model Feature Importance")
    plt.tight_layout()
    plt.savefig(MODEL_DIR / "feature_importance.png", dpi=160)


def model_version() -> str:
    return "ranking-xgb-" + datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")


if __name__ == "__main__":
    main()
