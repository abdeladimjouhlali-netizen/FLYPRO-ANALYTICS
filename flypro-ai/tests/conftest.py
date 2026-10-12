from __future__ import annotations

from datetime import datetime, timedelta, timezone
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_prediction_service, get_registry, get_training_service
from app.core.config import Settings
from app.main import create_app
from app.models.enums import AccessDirection, AccessResult, BadgeStatus, EmployeeStatus
from app.schemas.train import AccessEventFeatures
from app.services.model_registry import ModelRegistry
from app.services.prediction_service import PredictionService
from app.services.rules_engine import BusinessRulesEngine
from app.services.training_service import TrainingService


def make_event(
    *,
    hour: int = 10,
    day_offset: int = 0,
    employee_id: int = 1,
    badge_id: int = 1,
    access_point_id: int = 1,
    zone_id: int = 1,
    access_result: AccessResult = AccessResult.AUTHORIZED,
    access_direction: AccessDirection = AccessDirection.ENTRY,
    denied_last_hour: int = 0,
    accesses_today: int = 3,
    employee_status: EmployeeStatus | None = EmployeeStatus.ACTIVE,
    badge_status: BadgeStatus | None = BadgeStatus.ACTIVE,
    previous_zone_id: int | None = None,
    minutes_since_previous_access: float | None = None,
    is_expected_zone: bool | None = True,
) -> AccessEventFeatures:
    base = datetime(2026, 7, 20, hour, 0, 0, tzinfo=timezone.utc) + timedelta(days=day_offset)
    return AccessEventFeatures(
        event_timestamp=base,
        employee_id=employee_id,
        badge_id=badge_id,
        access_point_id=access_point_id,
        zone_id=zone_id,
        access_result=access_result,
        access_direction=access_direction,
        denied_last_hour=denied_last_hour,
        accesses_today=accesses_today,
        employee_status=employee_status,
        badge_status=badge_status,
        previous_zone_id=previous_zone_id,
        minutes_since_previous_access=minutes_since_previous_access,
        is_expected_zone=is_expected_zone,
    )


def make_training_events(n: int = 40) -> list[AccessEventFeatures]:
    events: list[AccessEventFeatures] = []
    for i in range(n):
        events.append(
            make_event(
                hour=8 + (i % 9),
                day_offset=i % 5,
                employee_id=1 + (i % 5),
                badge_id=1 + (i % 4),
                access_point_id=1 + (i % 3),
                zone_id=1 + (i % 2),
                accesses_today=2 + (i % 6),
                denied_last_hour=0 if i % 7 else 1,
            )
        )
    return events


@pytest.fixture
def settings(tmp_path: Path) -> Settings:
    return Settings(
        models_dir=str(tmp_path / "models"),
        min_training_samples=20,
        contamination=0.1,
        n_estimators=50,
        random_state=42,
    )


@pytest.fixture
def registry(settings: Settings) -> ModelRegistry:
    return ModelRegistry(settings)


@pytest.fixture
def training_service(settings: Settings, registry: ModelRegistry) -> TrainingService:
    return TrainingService(settings, registry)


@pytest.fixture
def prediction_service(settings: Settings, registry: ModelRegistry) -> PredictionService:
    return PredictionService(registry, BusinessRulesEngine(settings))


@pytest.fixture
def client(settings: Settings, registry: ModelRegistry, monkeypatch: pytest.MonkeyPatch) -> TestClient:
    # Lifespan uses get_registry imported in app.main
    monkeypatch.setattr("app.main.get_registry", lambda: registry)
    monkeypatch.setattr("app.main.get_settings", lambda: settings)

    app = create_app()
    app.dependency_overrides[get_registry] = lambda: registry
    app.dependency_overrides[get_training_service] = lambda: TrainingService(settings, registry)
    app.dependency_overrides[get_prediction_service] = lambda: PredictionService(
        registry, BusinessRulesEngine(settings)
    )

    with TestClient(app) as test_client:
        yield test_client

    app.dependency_overrides.clear()
