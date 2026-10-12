from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


class CamelModel(BaseModel):
    """Base schema with camelCase JSON aliases for Spring Boot compatibility."""

    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=to_camel,
        extra="ignore",
    )


class ApiResponse(CamelModel):
    success: bool = True
    message: str | None = None
    data: Any = None
