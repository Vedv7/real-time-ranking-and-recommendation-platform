from __future__ import annotations

import time

from fastapi import FastAPI

from app.model import RankingModel
from app.schemas import PredictionRequest, PredictionResponse

app = FastAPI(
    title="Ranking ML Inference Service",
    description="Serves click-through-rate predictions for personalized feed ranking.",
    version="0.1.0",
)
model = RankingModel()


@app.get("/health")
def health() -> dict[str, object]:
    return {"status": "ok", "using_fallback": model.using_fallback}


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictionRequest) -> PredictionResponse:
    started = time.perf_counter()
    predictions = model.predict(request.features)
    latency_ms = round((time.perf_counter() - started) * 1000, 3)
    print(
        "ml_inference "
        f"items={len(request.features)} latency_ms={latency_ms} using_fallback={model.using_fallback}",
        flush=True,
    )
    return PredictionResponse(predictions=predictions)
