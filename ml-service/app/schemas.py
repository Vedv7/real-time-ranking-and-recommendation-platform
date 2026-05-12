from pydantic import BaseModel, Field


class RankingFeatures(BaseModel):
    user_total_interactions: int = Field(ge=0)
    user_avg_watch_time: float = Field(ge=0)
    user_like_rate: float = Field(ge=0, le=1)
    user_skip_rate: float = Field(ge=0, le=1)
    content_popularity_score: float = Field(ge=0, le=1)
    content_freshness_score: float = Field(ge=0, le=1)
    content_engagement_rate: float = Field(ge=0, le=1)
    category_match: int = Field(ge=0, le=1)
    creator_affinity: float = Field(ge=0, le=1)
    content_age_hours: float = Field(ge=0)


class PredictionRequest(BaseModel):
    features: list[RankingFeatures]


class Prediction(BaseModel):
    predicted_ctr: float
    confidence: float


class PredictionResponse(BaseModel):
    predictions: list[Prediction]
    model_version: str
    model_stage: str
    using_fallback: bool
    loaded_at: str


class ModelMetadata(BaseModel):
    model_version: str
    model_stage: str
    using_fallback: bool
    loaded_at: str
