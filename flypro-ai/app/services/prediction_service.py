from __future__ import annotations

import numpy as np

from app.core.logging import get_logger
from app.models.enums import PredictionLabel
from app.schemas.predict import PredictResponse
from app.schemas.train import AccessEventFeatures
from app.services.model_registry import ModelRegistry
from app.services.rules_engine import BusinessRulesEngine

logger = get_logger(__name__)


class PredictionService:
    """Combines Isolation Forest scores with business-rule explanations."""

    def __init__(self, registry: ModelRegistry, rules: BusinessRulesEngine) -> None:
        self._registry = registry
        self._rules = rules

    def predict_batch(self, events: list[AccessEventFeatures]) -> list[PredictResponse]:
        if not events:
            return []
        if not self._registry.is_loaded:
            self._registry.load_latest()

        # Vectorized transform + model scoring (single pass) then per-event rules
        x = self._registry.preprocessor.transform(events)
        raw_labels = self._registry.detector.predict(x)
        decisions = self._registry.detector.decision_function(x)
        return [
            self._compose(event, int(raw_labels[i]), float(decisions[i]))
            for i, event in enumerate(events)
        ]

    def predict(self, event: AccessEventFeatures) -> PredictResponse:
        if not self._registry.is_loaded:
            self._registry.load_latest()

        x = self._registry.preprocessor.transform([event])
        raw_label = int(self._registry.detector.predict(x)[0])
        decision = float(self._registry.detector.decision_function(x)[0])
        return self._compose(event, raw_label, decision)

    def _compose(self, event: AccessEventFeatures, raw_label: int, decision: float) -> PredictResponse:
        ml_risk = self._decision_to_risk(decision)
        rules = self._rules.evaluate(event)

        risk_score = float(np.clip(max(ml_risk, rules.severity), 0.0, 1.0))
        if rules.severity > 0 and ml_risk > 0.4:
            risk_score = float(np.clip(0.6 * ml_risk + 0.4 * rules.severity + 0.1, 0.0, 1.0))

        is_anomaly = raw_label == -1 or rules.severity >= 0.35 or risk_score >= 0.65
        prediction = PredictionLabel.ANOMALY if is_anomaly else PredictionLabel.NORMAL
        confidence = self._confidence(risk_score, is_anomaly, decision)

        reason = self._build_reason(prediction, rules.reasons, ml_risk)
        action = self._rules.recommend_action(is_anomaly, risk_score, rules.flags)
        version = self._registry.metadata.version if self._registry.metadata else None

        logger.debug(
            "Prediction label=%s risk=%.3f confidence=%.1f flags=%s",
            prediction.value,
            risk_score,
            confidence,
            rules.flags,
        )

        return PredictResponse(
            prediction=prediction,
            risk_score=round(risk_score, 4),
            confidence=round(confidence, 2),
            reason=reason,
            recommended_action=action,
            model_version=version,
            rule_flags=rules.flags,
            anomaly_score=round(decision, 6),
        )

    @staticmethod
    def _decision_to_risk(decision: float) -> float:
        # Map typical decision range roughly [-0.5, 0.5] into [0, 1] risk
        risk = 0.5 - decision
        return float(np.clip(risk, 0.0, 1.0))

    @staticmethod
    def _confidence(risk_score: float, is_anomaly: bool, decision: float) -> float:
        distance = abs(decision)
        base = 55.0 + min(distance, 0.5) * 80.0
        if is_anomaly:
            base = max(base, 50.0 + risk_score * 50.0)
        else:
            base = max(base, 50.0 + (1.0 - risk_score) * 45.0)
        return float(np.clip(base, 50.0, 99.9))

    @staticmethod
    def _build_reason(prediction: PredictionLabel, reasons: list[str], ml_risk: float) -> str:
        if reasons:
            joined = " with ".join(reasons[:2]) if len(reasons) > 1 else reasons[0]
            if len(reasons) > 2:
                joined = f"{joined}; additional signals detected"
            return f"{joined}."
        if prediction == PredictionLabel.ANOMALY:
            return (
                "Statistical anomaly detected by Isolation Forest "
                f"(elevated deviation score {ml_risk:.2f})."
            )
        return "Access pattern is consistent with historical normal behaviour."
