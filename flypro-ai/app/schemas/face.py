from pydantic import Field

from app.schemas.common import CamelModel


class FaceEnrollRequest(CamelModel):
    """Un échantillon de visage à associer à un employé."""

    employee_id: int = Field(..., ge=1, examples=[42])
    image_base64: str = Field(..., description="Photo encodée en base64 (JPEG/PNG), avec ou sans préfixe data URI")


class FaceEnrollResponse(CamelModel):
    employee_id: int
    enrolled: bool
    samples_count: int


class FaceVerifyRequest(CamelModel):
    """Capture en direct à comparer au visage enrôlé pour cet employé."""

    employee_id: int = Field(..., ge=1, examples=[42])
    image_base64: str = Field(..., description="Photo encodée en base64 (JPEG/PNG), avec ou sans préfixe data URI")


class FaceVerifyResponse(CamelModel):
    employee_id: int
    matched: bool
    confidence: float = Field(..., ge=0, le=1)
    distance: float
    message: str
