from fastapi import APIRouter, Depends, status

from app.api.deps import get_face_service
from app.schemas.common import ApiResponse
from app.schemas.face import FaceEnrollRequest, FaceEnrollResponse, FaceVerifyRequest, FaceVerifyResponse
from app.services.face_service import FaceRecognitionService

router = APIRouter(prefix="/face", tags=["Face Recognition"])


@router.post(
    "/enroll",
    response_model=ApiResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Enrôler le visage d'un employé",
    responses={
        201: {
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "message": "Face enrolled successfully",
                        "data": {"employeeId": 42, "enrolled": True, "samplesCount": 1},
                    }
                }
            }
        }
    },
)
def enroll(
    request: FaceEnrollRequest,
    service: FaceRecognitionService = Depends(get_face_service),
) -> ApiResponse:
    result = service.enroll(request.employee_id, request.image_base64)
    return ApiResponse(success=True, message="Face enrolled successfully", data=result.model_dump(by_alias=True))


@router.post(
    "/verify",
    response_model=ApiResponse,
    summary="Vérifier une capture en direct par rapport au visage enrôlé",
    responses={
        200: {
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "message": "Visage reconnu : correspond au profil enrôlé pour cet employé.",
                        "data": {
                            "employeeId": 42,
                            "matched": True,
                            "confidence": 0.87,
                            "distance": 19.5,
                            "message": "Visage reconnu : correspond au profil enrôlé pour cet employé.",
                        },
                    }
                }
            }
        }
    },
)
def verify(
    request: FaceVerifyRequest,
    service: FaceRecognitionService = Depends(get_face_service),
) -> ApiResponse:
    result: FaceVerifyResponse = service.verify(request.employee_id, request.image_base64)
    return ApiResponse(success=True, message=result.message, data=result.model_dump(by_alias=True))
