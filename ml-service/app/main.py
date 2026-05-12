from __future__ import annotations

import time

from fastapi import FastAPI

from app.model import RankingModel
from app.schemas import ModelMetadata, PredictionRequest, PredictionResponse

app = FastAPI(
    title="Ranking ML Inference Service",
    description="Serves click-through-rate predictions for personalized feed ranking.",
    version="0.1.0",
)
model = RankingModel()


@app.get("/health")
def health() -> dict[str, object]:
    return {"status": "ok", **model.metadata().model_dump()}


@app.get("/metadata", response_model=ModelMetadata)
def metadata() -> ModelMetadata:
    return model.metadata()


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
    metadata = model.metadata()
    return PredictionResponse(
        predictions=predictions,
        model_version=metadata.model_version,
        model_stage=metadata.model_stage,
        using_fallback=metadata.using_fallback,
        loaded_at=metadata.loaded_at,
    )
