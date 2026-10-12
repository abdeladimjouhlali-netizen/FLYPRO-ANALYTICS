from __future__ import annotations

from typing import Iterable

import numpy as np
import pandas as pd

from app.models.enums import AccessDirection, AccessResult
from app.schemas.train import AccessEventFeatures

FEATURE_COLUMNS: list[str] = [
    "hour",
    "day_of_week",
    "employee_id",
    "badge_id",
    "access_point_id",
    "zone_id",
    "access_result_code",
    "access_direction_code",
    "denied_last_hour",
    "accesses_today",
]

RESULT_ENCODING: dict[str, int] = {
    AccessResult.AUTHORIZED.value: 0,
    AccessResult.DENIED.value: 1,
    AccessResult.EXPIRED.value: 2,
    AccessResult.UNKNOWN.value: 3,
    AccessResult.TECHNICAL_ERROR.value: 4,
}

DIRECTION_ENCODING: dict[str, int] = {
    AccessDirection.ENTRY.value: 0,
    AccessDirection.EXIT.value: 1,
}


def events_to_dataframe(events: Iterable[AccessEventFeatures]) -> pd.DataFrame:
    """Convert validated events into a feature-ready DataFrame."""
    rows: list[dict[str, float | int]] = []
    for event in events:
        ts = event.event_timestamp
        result = event.access_result.value if hasattr(event.access_result, "value") else str(event.access_result)
        direction = (
            event.access_direction.value
            if hasattr(event.access_direction, "value")
            else str(event.access_direction)
        )
        rows.append(
            {
                "hour": int(ts.hour),
                "day_of_week": int(ts.weekday()),
                "employee_id": int(event.employee_id),
                "badge_id": int(event.badge_id),
                "access_point_id": int(event.access_point_id),
                "zone_id": int(event.zone_id),
                "access_result_code": RESULT_ENCODING.get(result, 3),
                "access_direction_code": DIRECTION_ENCODING.get(direction, 0),
                "denied_last_hour": int(event.denied_last_hour or 0),
                "accesses_today": int(event.accesses_today or 0),
            }
        )
    frame = pd.DataFrame(rows, columns=FEATURE_COLUMNS)
    return frame.fillna(0)


def dataframe_to_matrix(frame: pd.DataFrame) -> np.ndarray:
    ordered = frame.reindex(columns=FEATURE_COLUMNS, fill_value=0)
    return ordered.to_numpy(dtype=np.float64)
