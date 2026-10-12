"""Uvicorn entrypoint for FlyPro AI Anomaly Detection Service."""

import uvicorn

from app.core.config import get_settings


def run() -> None:
    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=False,
        log_level=settings.log_level.lower(),
    )


if __name__ == "__main__":
    run()
