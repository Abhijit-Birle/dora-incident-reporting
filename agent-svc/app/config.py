from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application configuration.
    Reads from .env file locally and from OS/Kubernetes environment variables in production.
    """
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

    service_name: str = "agent-svc"
    environment: str = "development"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    incident_svc_url: str = "http://localhost:8080"


settings = Settings()
