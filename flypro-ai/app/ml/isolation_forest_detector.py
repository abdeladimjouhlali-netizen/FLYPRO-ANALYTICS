from __future__ import annotations

import numpy as np
from sklearn.ensemble import IsolationForest

from app.ml.base import AnomalyDetector


class IsolationForestDetector(AnomalyDetector):
    """Isolation Forest wrapper implementing the AnomalyDetector contract."""

    def __init__(
        self,
        *,
        contamination: float = 0.05,
        n_estimators: int = 200,
        random_state: int = 42,
    ) -> None:
        self.contamination = contamination
        self.n_estimators = n_estimators
        self.random_state = random_state
        self._model = IsolationForest(
            n_estimators=n_estimators,
            contamination=contamination,
            random_state=random_state,
            n_jobs=-1,
        )
        self._fitted = False

    @property
    def is_fitted(self) -> bool:
        return self._fitted

    def fit(self, x: np.ndarray) -> None:
        self._model.fit(x)
        self._fitted = True

    def decision_function(self, x: np.ndarray) -> np.ndarray:
        return self._model.decision_function(x)

    def predict(self, x: np.ndarray) -> np.ndarray:
        return self._model.predict(x)

    def score_samples(self, x: np.ndarray) -> np.ndarray:
        return self._model.score_samples(x)
