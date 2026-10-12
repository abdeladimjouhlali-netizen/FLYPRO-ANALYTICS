from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application configuration loaded from environment variables."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "FlyPro AI Anomaly Detection"
    app_version: str = "1.0.0"
    api_prefix: str = "/api/v1/ai"
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"

    models_dir: str = "artifacts/models"
    model_filename_prefix: str = "isolation_forest"
    metadata_filename: str = "latest.json"

    contamination: float = 0.05
    n_estimators: int = 200
    random_state: int = 42
    min_training_samples: int = 20

    workday_start_hour: int = 8
    workday_end_hour: int = 18

    # Comma-separated origins; empty disables CORS middleware origins list
    cors_allowed_origins: str = (
        "http://localhost,http://localhost:80,http://localhost:3000,"
        "http://localhost:5173,http://127.0.0.1"
    )

    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
