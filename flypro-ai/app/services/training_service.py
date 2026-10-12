from __future__ import annotations

import time
from datetime import datetime, timezone

import numpy as np

from app.core.config import Settings
from app.core.exceptions import InsufficientDataError, InvalidTrainingDataError
from app.core.logging import get_logger
from app.ml.base import ModelMetadata
from app.ml.features import FEATURE_COLUMNS
from app.ml.isolation_forest_detector import IsolationForestDetector
from app.ml.preprocessor import FeaturePreprocessor
from app.schemas.train import EvaluateRequest, RetrainRequest, TrainRequest, TrainResponse
from app.services.model_registry import ModelRegistry

logger = get_logger(__name__)


class TrainingService:
    """Trains, evaluates and persists Isolation Forest models."""

    def __init__(self, settings: Settings, registry: ModelRegistry) -> None:
        self._settings = settings
        self._registry = registry

    def train(self, request: TrainRequest) -> TrainResponse:
        return self._fit_and_persist(request, action="train")

    def retrain(self, request: RetrainRequest) -> TrainResponse:
        logger.info("Retraining requested with %s samples", len(request.events))
        return self._fit_and_persist(request, action="retrain")

    def evaluate(self, request: EvaluateRequest) -> dict[str, float | int]:
        if not self._registry.is_loaded:
            self._registry.load_latest()
        x = self._registry.preprocessor.transform(request.events)
        preds = self._registry.detector.predict(x)
        scores = self._registry.detector.decision_function(x)
        return self._compute_metrics(preds, scores)

    def load_model(self, version: str | None = None) -> ModelMetadata:
        if version:
            return self._registry.load_version(version)
        return self._registry.load_latest()

    def save_current(self) -> ModelMetadata:
        if not self._registry.is_loaded or self._registry.metadata is None:
            raise InvalidTrainingDataError("No in-memory model to save")
        return self._registry.save(
            self._registry.detector,
            self._registry.preprocessor,
            self._registry.metadata,
        )

    def _fit_and_persist(self, request: TrainRequest, *, action: str) -> TrainResponse:
        events = request.events
        if len(events) < self._settings.min_training_samples:
            raise InsufficientDataError(
                f"At least {self._settings.min_training_samples} events are required for training"
            )

        contamination = request.contamination or self._settings.contamination
        if not 0.0 < contamination < 0.5:
            raise InvalidTrainingDataError("contamination must be between 0 and 0.5")

        logger.info("%s started samples=%s contamination=%s", action, len(events), contamination)
        started = time.perf_counter()

        preprocessor = FeaturePreprocessor()
        x = preprocessor.fit_transform(events)

        detector = IsolationForestDetector(
            contamination=contamination,
            n_estimators=self._settings.n_estimators,
            random_state=self._settings.random_state,
        )
        detector.fit(x)

        preds = detector.predict(x)
        scores = detector.decision_function(x)
        metrics = self._compute_metrics(preds, scores)

        version = ModelRegistry.next_version(request.version_label)
        metadata = ModelMetadata(
            version=version,
            trained_at=datetime.now(timezone.utc),
            samples=len(events),
            features=list(FEATURE_COLUMNS),
            contamination=contamination,
            metrics=metrics,
            label=request.version_label,
        )
        saved = self._registry.save(detector, preprocessor, metadata)
        duration = time.perf_counter() - started
        logger.info("%s completed version=%s duration=%.3fs", action, saved.version, duration)

        return TrainResponse(
            model_version=saved.version,
            samples=saved.samples,
            features=saved.features,
            contamination=saved.contamination,
            training_duration_seconds=round(duration, 4),
            artifact_path=saved.artifact_path,
            evaluation=metrics,
        )

    @staticmethod
    def _compute_metrics(preds: np.ndarray, scores: np.ndarray) -> dict[str, float | int]:
        anomaly_count = int(np.sum(preds == -1))
        normal_count = int(np.sum(preds == 1))
        return {
            "samples": int(len(preds)),
            "anomaly_count": anomaly_count,
            "normal_count": normal_count,
            "anomaly_ratio": round(float(anomaly_count / max(len(preds), 1)), 4),
            "score_mean": round(float(np.mean(scores)), 6),
            "score_std": round(float(np.std(scores)), 6),
            "score_min": round(float(np.min(scores)), 6),
            "score_max": round(float(np.max(scores)), 6),
        }
