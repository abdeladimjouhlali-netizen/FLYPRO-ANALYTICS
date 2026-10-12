from datetime import datetime, timezone

from pydantic import Field, field_validator

from app.models.enums import AccessDirection, AccessResult, BadgeStatus, EmployeeStatus
from app.schemas.common import CamelModel


class AccessEventFeatures(CamelModel):
    """Single access event payload used for training and prediction."""

    event_timestamp: datetime = Field(
        ...,
        description="ISO-8601 timestamp of the access attempt",
        examples=["2026-07-20T22:15:00Z"],
    )
    employee_id: int = Field(..., ge=1, examples=[42])
    badge_id: int = Field(..., ge=1, examples=[7])
    access_point_id: int = Field(..., ge=1, examples=[3])
    zone_id: int = Field(..., ge=1, examples=[2])
    access_result: AccessResult = Field(..., examples=["AUTHORIZED"])
    access_direction: AccessDirection = Field(..., examples=["ENTRY"])
    denied_last_hour: int = Field(
        0,
        ge=0,
        description="Number of denied accesses for this badge/employee in the last hour",
        examples=[3],
    )
    accesses_today: int = Field(
        0,
        ge=0,
        description="Number of accesses for this employee during the current day",
        examples=[12],
    )

    # Optional context for business-rule enrichment (not required by Isolation Forest)
    employee_status: EmployeeStatus | None = Field(None, examples=["ACTIVE"])
    badge_status: BadgeStatus | None = Field(None, examples=["ACTIVE"])
    previous_zone_id: int | None = Field(
        None,
        ge=1,
        description="Previous zone of the same employee (impossible travel detection)",
    )
    minutes_since_previous_access: float | None = Field(
        None,
        ge=0,
        description="Minutes since previous access (used with zone change)",
    )
    is_expected_zone: bool | None = Field(
        True,
        description="Whether the zone is authorized by the employee access profile",
    )

    @field_validator("event_timestamp")
    @classmethod
    def ensure_timezone_aware(cls, value: datetime) -> datetime:
        if value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value.astimezone(timezone.utc)


class TrainRequest(CamelModel):
    events: list[AccessEventFeatures] = Field(..., min_length=1)
    contamination: float | None = Field(
        None,
        gt=0.0,
        lt=0.5,
        description="Isolation Forest contamination override",
    )
    version_label: str | None = Field(
        None,
        max_length=64,
        description="Optional human-readable version label",
    )


class TrainResponse(CamelModel):
    model_version: str
    samples: int
    features: list[str]
    contamination: float
    training_duration_seconds: float
    artifact_path: str
    evaluation: dict[str, float | int]


class RetrainRequest(TrainRequest):
    """Retrain using a new dataset; replaces the latest model after success."""


class EvaluateRequest(CamelModel):
    events: list[AccessEventFeatures] = Field(..., min_length=1)
