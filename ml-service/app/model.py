from __future__ import annotations

from pathlib import Path

import joblib
import numpy as np

from app.features import deterministic_score, to_frame
from app.schemas import Prediction, RankingFeatures


MODEL_PATH = Path(__file__).resolve().parents[1] / "models" / "ranking_model.joblib"


class RankingModel:
    def __init__(self, model_path: Path = MODEL_PATH) -> None:
        self.model_path = model_path
        self.model = None
        self.load()

    def load(self) -> None:
        if self.model_path.exists():
            self.model = joblib.load(self.model_path)

    @property
    def using_fallback(self) -> bool:
        return self.model is None

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
