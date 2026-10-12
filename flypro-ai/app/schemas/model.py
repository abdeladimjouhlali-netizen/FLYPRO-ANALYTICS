from datetime import datetime

from app.schemas.common import CamelModel


class ModelInfoResponse(CamelModel):
    loaded: bool
    model_version: str | None = None
    trained_at: datetime | None = None
    samples: int | None = None
    features: list[str] = []
    contamination: float | None = None
    algorithm: str = "IsolationForest"
    artifact_path: str | None = None
    available_versions: list[str] = []


class HealthResponse(CamelModel):
    status: str
    service: str
    version: str
    model_loaded: bool
    model_version: str | None = None
