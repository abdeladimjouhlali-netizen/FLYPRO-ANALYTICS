from fastapi import APIRouter, Depends, Query, status

from app import __version__
from app.api.deps import get_prediction_service, get_registry, get_training_service
from app.schemas.common import ApiResponse
from app.schemas.model import HealthResponse, ModelInfoResponse
from app.schemas.predict import BatchPredictRequest, PredictRequest, PredictResponse
from app.schemas.train import EvaluateRequest, RetrainRequest, TrainRequest, TrainResponse
from app.services.model_registry import ModelRegistry
from app.services.prediction_service import PredictionService
from app.services.training_service import TrainingService

router = APIRouter(tags=["AI Anomaly Detection"])


@router.get(
    "/health",
    response_model=ApiResponse,
    summary="Health check",
    responses={
        200: {
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "message": "Service is healthy",
                        "data": {
                            "status": "UP",
                            "service": "FlyPro AI Anomaly Detection",
                            "version": "1.0.0",
                            "modelLoaded": True,
                            "modelVersion": "20260727120000",
                        },
                    }
                }
            }
        }
    },
)
def health(registry: ModelRegistry = Depends(get_registry)) -> ApiResponse:
    meta = registry.metadata
    payload = HealthResponse(
        status="UP",
        service="FlyPro AI Anomaly Detection",
        version=__version__,
        model_loaded=registry.is_loaded,
        model_version=meta.version if meta else None,
    )
    return ApiResponse(
        success=True,
        message="Service is healthy",
        data=payload.model_dump(by_alias=True),
    )


@router.get(
    "/model",
    response_model=ApiResponse,
    summary="Get current model metadata",
)
def get_model(registry: ModelRegistry = Depends(get_registry)) -> ApiResponse:
    meta = registry.metadata
    info = ModelInfoResponse(
        loaded=registry.is_loaded,
        model_version=meta.version if meta else None,
        trained_at=meta.trained_at if meta else None,
        samples=meta.samples if meta else None,
        features=meta.features if meta else [],
        contamination=meta.contamination if meta else None,
        artifact_path=meta.artifact_path if meta else None,
        available_versions=registry.list_versions(),
    )
    return ApiResponse(
        success=True,
        message="Model metadata retrieved",
        data=info.model_dump(by_alias=True, mode="json"),
    )


@router.post(
    "/model/load",
    response_model=ApiResponse,
    summary="Load a trained model version (latest if omitted)",
)
def load_model(
    version: str | None = Query(None, description="Model version to load"),
    training: TrainingService = Depends(get_training_service),
) -> ApiResponse:
    meta = training.load_model(version)
    return ApiResponse(
        success=True,
        message=f"Model '{meta.version}' loaded successfully",
        data=meta.to_dict(),
    )


@router.post(
    "/model/save",
    response_model=ApiResponse,
    summary="Persist the currently loaded model to disk",
)
def save_model(training: TrainingService = Depends(get_training_service)) -> ApiResponse:
    meta = training.save_current()
    return ApiResponse(
        success=True,
        message=f"Model '{meta.version}' saved successfully",
        data=meta.to_dict(),
    )


@router.post(
    "/train",
    response_model=ApiResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Train a new Isolation Forest model",
)
def train(
    request: TrainRequest,
    training: TrainingService = Depends(get_training_service),
) -> ApiResponse:
    result: TrainResponse = training.train(request)
    return ApiResponse(
        success=True,
        message="Model trained successfully",
        data=result.model_dump(by_alias=True),
    )


@router.post(
    "/retrain",
    response_model=ApiResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Retrain the model with a new dataset",
)
def retrain(
    request: RetrainRequest,
    training: TrainingService = Depends(get_training_service),
) -> ApiResponse:
    result = training.retrain(request)
    return ApiResponse(
        success=True,
        message="Model retrained successfully",
        data=result.model_dump(by_alias=True),
    )


@router.post(
    "/evaluate",
    response_model=ApiResponse,
    summary="Evaluate the loaded model on a dataset",
)
def evaluate(
    request: EvaluateRequest,
    training: TrainingService = Depends(get_training_service),
) -> ApiResponse:
    metrics = training.evaluate(request)
    return ApiResponse(success=True, message="Evaluation completed", data=metrics)


@router.post(
    "/predict",
    response_model=ApiResponse,
    summary="Predict anomaly for a single access event",
    responses={
        200: {
            "content": {
                "application/json": {
                    "example": {
                        "success": True,
                        "message": "Prediction completed",
                        "data": {
                            "prediction": "ANOMALY",
                            "riskScore": 0.94,
                            "confidence": 98.5,
                            "reason": "Access outside normal working hours with repeated denied attempts.",
                            "recommendedAction": "Investigate immediately",
                            "modelVersion": "20260727120000",
                            "ruleFlags": ["OUTSIDE_WORKING_HOURS", "REPEATED_DENIED_ACCESSES"],
                            "anomalyScore": -0.12,
                        },
                    }
                }
            }
        }
    },
)
def predict(
    request: PredictRequest,
    prediction: PredictionService = Depends(get_prediction_service),
) -> ApiResponse:
    result: PredictResponse = prediction.predict(request.event)
    return ApiResponse(
        success=True,
        message="Prediction completed",
        data=result.model_dump(by_alias=True),
    )


@router.post(
    "/predict/batch",
    response_model=ApiResponse,
    summary="Predict anomalies for multiple access events",
)
def predict_batch(
    request: BatchPredictRequest,
    prediction: PredictionService = Depends(get_prediction_service),
) -> ApiResponse:
    results = prediction.predict_batch(request.events)
    return ApiResponse(
        success=True,
        message="Batch prediction completed",
        data=[item.model_dump(by_alias=True) for item in results],
    )
