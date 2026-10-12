from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import joblib

from app.core.config import Settings
from app.core.exceptions import ModelLoadError, ModelNotFoundError
from app.core.logging import get_logger
from app.ml.base import ModelMetadata
from app.ml.isolation_forest_detector import IsolationForestDetector
from app.ml.preprocessor import FeaturePreprocessor, PreprocessorState

logger = get_logger(__name__)


class ModelRegistry:
    """Persists and versions trained Isolation Forest artifacts with Joblib."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._models_dir = Path(settings.models_dir)
        self._models_dir.mkdir(parents=True, exist_ok=True)
        self._detector: IsolationForestDetector | None = None
        self._preprocessor: FeaturePreprocessor | None = None
        self._metadata: ModelMetadata | None = None

    @property
    def is_loaded(self) -> bool:
        return self._detector is not None and self._preprocessor is not None and self._metadata is not None

    @property
    def detector(self) -> IsolationForestDetector:
        if self._detector is None:
            raise ModelNotFoundError()
        return self._detector

    @property
    def preprocessor(self) -> FeaturePreprocessor:
        if self._preprocessor is None:
            raise ModelNotFoundError()
        return self._preprocessor

    @property
    def metadata(self) -> ModelMetadata | None:
        return self._metadata

    def list_versions(self) -> list[str]:
        versions = sorted(
            p.stem.replace(f"{self._settings.model_filename_prefix}_", "")
            for p in self._models_dir.glob(f"{self._settings.model_filename_prefix}_*.joblib")
        )
        return versions

    def save(
        self,
        detector: IsolationForestDetector,
        preprocessor: FeaturePreprocessor,
        metadata: ModelMetadata,
    ) -> ModelMetadata:
        version = metadata.version
        artifact = self._models_dir / f"{self._settings.model_filename_prefix}_{version}.joblib"
        payload = {
            "detector": detector,
            "preprocessor_state": preprocessor.to_state(),
            "metadata": metadata.to_dict(),
        }
        joblib.dump(payload, artifact)
        metadata.artifact_path = str(artifact)
        payload["metadata"] = metadata.to_dict()
        joblib.dump(payload, artifact)

        latest_path = self._models_dir / self._settings.metadata_filename
        latest_path.write_text(json.dumps(metadata.to_dict(), indent=2), encoding="utf-8")

        self._detector = detector
        self._preprocessor = preprocessor
        self._metadata = metadata
        logger.info("Saved model version=%s path=%s", version, artifact)
        return metadata

    def load_latest(self) -> ModelMetadata:
        latest_path = self._models_dir / self._settings.metadata_filename
        if not latest_path.exists():
            raise ModelNotFoundError("No latest model metadata found")
        try:
            data = json.loads(latest_path.read_text(encoding="utf-8"))
            return self.load_version(data["version"])
        except ModelNotFoundError:
            raise
        except Exception as exc:  # noqa: BLE001
            raise ModelLoadError(f"Failed to load latest model: {exc}") from exc

    def load_version(self, version: str) -> ModelMetadata:
        artifact = self._models_dir / f"{self._settings.model_filename_prefix}_{version}.joblib"
        if not artifact.exists():
            raise ModelNotFoundError(f"Model version '{version}' not found")
        try:
            payload: dict[str, Any] = joblib.load(artifact)
            detector: IsolationForestDetector = payload["detector"]
            preprocessor = FeaturePreprocessor.from_state(payload["preprocessor_state"])
            metadata = ModelMetadata.from_dict(payload["metadata"])
            self._detector = detector
            self._preprocessor = preprocessor
            self._metadata = metadata
            logger.info("Loaded model version=%s", version)
            return metadata
        except Exception as exc:  # noqa: BLE001
            raise ModelLoadError(f"Failed to load model '{version}': {exc}") from exc

    def try_load_latest(self) -> bool:
        try:
            self.load_latest()
            return True
        except (ModelNotFoundError, ModelLoadError) as exc:
            logger.warning("No model loaded at startup: %s", exc.message)
            return False

    @staticmethod
    def next_version(label: str | None = None) -> str:
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
        if label:
            safe = "".join(ch if ch.isalnum() or ch in "-_" else "-" for ch in label)[:32]
            return f"{stamp}-{safe}"
        return stamp
