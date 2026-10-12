from app.models.enums import AccessResult, BadgeStatus, EmployeeStatus, PredictionLabel
from app.schemas.train import TrainRequest
from app.services.prediction_service import PredictionService
from app.services.rules_engine import BusinessRulesEngine
from app.services.training_service import TrainingService
from tests.conftest import make_event, make_training_events


def test_predict_normal_event(training_service: TrainingService, prediction_service: PredictionService):
    training_service.train(TrainRequest(events=make_training_events(40)))
    result = prediction_service.predict(make_event(hour=10, denied_last_hour=0, accesses_today=3))
    assert result.prediction in {PredictionLabel.NORMAL, PredictionLabel.ANOMALY}
    assert 0.0 <= result.risk_score <= 1.0
    assert 0.0 <= result.confidence <= 100.0
    assert result.reason


def test_predict_flags_business_anomaly(training_service: TrainingService, prediction_service: PredictionService):
    training_service.train(TrainRequest(events=make_training_events(40)))
    result = prediction_service.predict(
        make_event(
            hour=23,
            access_result=AccessResult.DENIED,
            denied_last_hour=5,
            badge_status=BadgeStatus.REVOKED,
            employee_status=EmployeeStatus.INACTIVE,
        )
    )
    assert result.prediction == PredictionLabel.ANOMALY
    assert result.risk_score >= 0.65
    assert "REVOKED_BADGE" in result.rule_flags
    assert "Investigate" in result.recommended_action.value or "Block" in result.recommended_action.value


def test_rules_impossible_travel(settings):
    engine = BusinessRulesEngine(settings)
    assessment = engine.evaluate(
        make_event(
            zone_id=2,
            previous_zone_id=9,
            minutes_since_previous_access=2,
        )
    )
    assert "IMPOSSIBLE_TRAVEL" in assessment.flags
