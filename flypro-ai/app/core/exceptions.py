from typing import Any


class AppException(Exception):
    """Base application exception."""

    def __init__(self, message: str, code: str = "APP_ERROR", status_code: int = 400) -> None:
        super().__init__(message)
        self.message = message
        self.code = code
        self.status_code = status_code


class ModelNotFoundError(AppException):
    def __init__(self, message: str = "No trained model is available") -> None:
        super().__init__(message, code="MODEL_NOT_FOUND", status_code=404)


class InsufficientDataError(AppException):
    def __init__(self, message: str) -> None:
        super().__init__(message, code="INSUFFICIENT_DATA", status_code=400)


class InvalidTrainingDataError(AppException):
    def __init__(self, message: str) -> None:
        super().__init__(message, code="INVALID_TRAINING_DATA", status_code=422)


class ModelLoadError(AppException):
    def __init__(self, message: str) -> None:
        super().__init__(message, code="MODEL_LOAD_ERROR", status_code=500)


class NoFaceDetectedError(AppException):
    def __init__(self, message: str = "No face detected in the provided image") -> None:
        super().__init__(message, code="NO_FACE_DETECTED", status_code=422)


class FaceNotEnrolledError(AppException):
    def __init__(self, message: str = "No enrolled face found for this employee") -> None:
        super().__init__(message, code="FACE_NOT_ENROLLED", status_code=404)


def error_payload(exc: AppException) -> dict[str, Any]:
    return {"success": False, "code": exc.code, "message": exc.message}
