from __future__ import annotations

from pathlib import Path
from datetime import datetime, timezone
import json

import joblib
import numpy as np

from app.features import deterministic_score, to_frame
from app.schemas import ModelMetadata, Prediction, RankingFeatures


MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "ranking_model.joblib"
METADATA_PATH = Path(__file__).resolve().parents[1] / "models" / "model_metadata.json"


class RankingModel:
    def __init__(self, model_path: Path = MODEL_PATH) -> None:
        self.model_path = model_path
        self.model = None
        self.loaded_at = datetime.now(timezone.utc).isoformat()
        self.model_version = "deterministic-fallback-v1"
        self.model_stage = "fallback"
        self.load()

    def load(self) -> None:
        if self.model_path.exists():
            self.model = joblib.load(self.model_path)
            self.model_stage = "production"
            self.model_version = self._metadata().get("model_version", "ranking-xgb-local")

    @property
    def using_fallback(self) -> bool:
        return self.model is None

    def metadata(self) -> ModelMetadata:
        return ModelMetadata(
            model_version=self.model_version,
            model_stage=self.model_stage,
            using_fallback=self.using_fallback,
            loaded_at=self.loaded_at,
        )

    def predict(self, features: list[RankingFeatures]) -> list[Prediction]:
        if not features:
            return []

        if self.model is None:
            probabilities = [deterministic_score(feature) for feature in features]
        else:
            frame = to_frame(features)
            probabilities = self.model.predict_proba(frame)[:, 1].astype(float).tolist()

        return [
            Prediction(
                predicted_ctr=round(float(probability), 4),
                confidence=self._confidence(float(probability)),
            )
            for probability in probabilities
        ]

    def _confidence(self, probability: float) -> float:
        if self.model is None:
            return round(0.55 + abs(probability - 0.5) * 0.8, 4)
        uncertainty = 1.0 - abs(probability - 0.5) * 2.0
        return round(float(np.clip(0.92 - 0.35 * uncertainty, 0.55, 0.95)), 4)

    def _metadata(self) -> dict[str, object]:
        if METADATA_PATH.exists():
            return json.loads(METADATA_PATH.read_text(encoding="utf-8"))
        return {}
