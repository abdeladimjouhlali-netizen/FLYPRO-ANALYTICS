from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any

import numpy as np


@dataclass
class ModelMetadata:
    version: str
    trained_at: datetime
    samples: int
    features: list[str]
    contamination: float
    algorithm: str = "IsolationForest"
    artifact_path: str = ""
    metrics: dict[str, float | int] = field(default_factory=dict)
    label: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "version": self.version,
            "trained_at": self.trained_at.astimezone(timezone.utc).isoformat(),
            "samples": self.samples,
            "features": self.features,
            "contamination": self.contamination,
            "algorithm": self.algorithm,
            "artifact_path": self.artifact_path,
            "metrics": self.metrics,
            "label": self.label,
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ModelMetadata:
        trained_at = data["trained_at"]
        if isinstance(trained_at, str):
            trained_at = datetime.fromisoformat(trained_at.replace("Z", "+00:00"))
        return cls(
            version=data["version"],
            trained_at=trained_at,
            samples=int(data["samples"]),
            features=list(data.get("features", [])),
            contamination=float(data["contamination"]),
            algorithm=data.get("algorithm", "IsolationForest"),
            artifact_path=data.get("artifact_path", ""),
            metrics=dict(data.get("metrics", {})),
            label=data.get("label"),
        )


class AnomalyDetector(ABC):
    """Extensible contract for future ML algorithms."""

    @abstractmethod
    def fit(self, x: np.ndarray) -> None:
        raise NotImplementedError

    @abstractmethod
    def decision_function(self, x: np.ndarray) -> np.ndarray:
        raise NotImplementedError

    @abstractmethod
    def predict(self, x: np.ndarray) -> np.ndarray:
        """Return 1 for normal, -1 for anomaly (sklearn convention)."""
        raise NotImplementedError
