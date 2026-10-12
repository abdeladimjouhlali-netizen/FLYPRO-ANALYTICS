from pydantic import Field

from app.models.enums import PredictionLabel, RecommendedAction
from app.schemas.common import CamelModel
from app.schemas.train import AccessEventFeatures


class PredictRequest(CamelModel):
    event: AccessEventFeatures


class BatchPredictRequest(CamelModel):
    events: list[AccessEventFeatures] = Field(..., min_length=1, max_length=500)


class PredictResponse(CamelModel):
    prediction: PredictionLabel = Field(..., examples=["ANOMALY"])
    risk_score: float = Field(..., ge=0.0, le=1.0, examples=[0.94])
    confidence: float = Field(..., ge=0.0, le=100.0, examples=[98.5])
    reason: str = Field(
        ...,
        examples=["Access outside normal working hours with repeated denied attempts."],
    )
    recommended_action: RecommendedAction = Field(
        ...,
        examples=["Investigate immediately"],
    )
    model_version: str | None = None
    rule_flags: list[str] = Field(default_factory=list)
    anomaly_score: float | None = Field(
        None,
        description="Raw Isolation Forest score (higher = more normal)",
    )
