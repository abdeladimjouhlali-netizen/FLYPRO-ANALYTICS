from functools import lru_cache

from app.core.config import Settings, get_settings
from app.services.face_service import FaceRecognitionService
from app.services.model_registry import ModelRegistry
from app.services.prediction_service import PredictionService
from app.services.rules_engine import BusinessRulesEngine
from app.services.training_service import TrainingService


@lru_cache
def get_registry() -> ModelRegistry:
    return ModelRegistry(get_settings())


@lru_cache
def get_rules_engine() -> BusinessRulesEngine:
    return BusinessRulesEngine(get_settings())


def get_training_service() -> TrainingService:
    return TrainingService(get_settings(), get_registry())


def get_prediction_service() -> PredictionService:
    return PredictionService(get_registry(), get_rules_engine())


def get_app_settings() -> Settings:
    return get_settings()


@lru_cache
def get_face_service() -> FaceRecognitionService:
    return FaceRecognitionService(get_settings())
