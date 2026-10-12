from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app import __version__
from app.api.deps import get_registry
from app.api.routers import ai as ai_router
from app.api.routers import face as face_router
from app.core.config import get_settings
from app.core.exceptions import AppException, error_payload
from app.core.logging import get_logger, setup_logging

logger = get_logger(__name__)


def create_app() -> FastAPI:
    setup_logging()
    settings = get_settings()

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        registry = get_registry()
        loaded = registry.try_load_latest()
        logger.info("FlyPro AI started version=%s model_loaded=%s", __version__, loaded)
        yield

    application = FastAPI(
        title=settings.app_name,
        version=__version__,
        description=(
            "Independent AI microservice for FlyPro Analytics. "
            "Detects anomalous badge access events using Isolation Forest "
            "and business rules. Data is received exclusively via REST APIs."
        ),
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )

    application.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list(),
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "Accept"],
    )

    application.include_router(ai_router.router, prefix=settings.api_prefix)
    application.include_router(face_router.router, prefix=settings.api_prefix)

    @application.exception_handler(AppException)
    async def handle_app_exception(_: Request, exc: AppException) -> JSONResponse:
        logger.error("Application error code=%s message=%s", exc.code, exc.message)
        return JSONResponse(status_code=exc.status_code, content=error_payload(exc))

    @application.exception_handler(RequestValidationError)
    async def handle_validation(_: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=422,
            content={
                "success": False,
                "code": "VALIDATION_ERROR",
                "message": "Request validation failed",
                "details": exc.errors(),
            },
        )

    @application.get("/", include_in_schema=False)
    def root() -> dict[str, str]:
        return {
            "service": settings.app_name,
            "version": __version__,
            "docs": "/docs",
            "health": f"{settings.api_prefix}/health",
        }

    return application


app = create_app()
