from app.core.exceptions import InsufficientDataError
from app.schemas.train import EvaluateRequest, TrainRequest
from app.services.training_service import TrainingService
from tests.conftest import make_event, make_training_events


def test_train_persists_model(training_service: TrainingService):
    response = training_service.train(
        TrainRequest(events=make_training_events(30), version_label="unit")
    )
    assert response.samples == 30
    assert response.model_version
    assert "anomaly_ratio" in response.evaluation
    assert training_service._registry.is_loaded  # noqa: SLF001


def test_train_rejects_small_dataset(training_service: TrainingService):
    try:
        training_service.train(TrainRequest(events=make_training_events(5)))
        raised = False
    except InsufficientDataError:
        raised = True
    assert raised


def test_retrain_and_evaluate(training_service: TrainingService):
    training_service.train(TrainRequest(events=make_training_events(30)))
    retrained = training_service.retrain(TrainRequest(events=make_training_events(35), version_label="v2"))
    assert retrained.samples == 35
    metrics = training_service.evaluate(EvaluateRequest(events=[make_event(), make_event(hour=22)]))
    assert metrics["samples"] == 2
