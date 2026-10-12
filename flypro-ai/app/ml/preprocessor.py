from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from sklearn.preprocessing import StandardScaler

from app.ml.features import FEATURE_COLUMNS, dataframe_to_matrix, events_to_dataframe
from app.schemas.train import AccessEventFeatures


@dataclass
class PreprocessorState:
    scaler: StandardScaler
    feature_names: list[str]


class FeaturePreprocessor:
    """Encoding, imputation and normalization pipeline for access events."""

    def __init__(self) -> None:
        self._scaler = StandardScaler()
        self._fitted = False
        self.feature_names: list[str] = list(FEATURE_COLUMNS)

    @property
    def is_fitted(self) -> bool:
        return self._fitted

    def fit(self, events: list[AccessEventFeatures]) -> "FeaturePreprocessor":
        matrix = dataframe_to_matrix(events_to_dataframe(events))
        self._scaler.fit(matrix)
        self._fitted = True
        return self

    def transform(self, events: list[AccessEventFeatures]) -> np.ndarray:
        if not self._fitted:
            raise RuntimeError("Preprocessor must be fitted before transform")
        matrix = dataframe_to_matrix(events_to_dataframe(events))
        # Handle potential NaN/inf after casting
        matrix = np.nan_to_num(matrix, nan=0.0, posinf=0.0, neginf=0.0)
        return self._scaler.transform(matrix)

    def fit_transform(self, events: list[AccessEventFeatures]) -> np.ndarray:
        self.fit(events)
        return self.transform(events)

    def to_state(self) -> PreprocessorState:
        return PreprocessorState(scaler=self._scaler, feature_names=self.feature_names)

    @classmethod
    def from_state(cls, state: PreprocessorState) -> "FeaturePreprocessor":
        instance = cls()
        instance._scaler = state.scaler
        instance.feature_names = list(state.feature_names)
        instance._fitted = True
        return instance
