"""CoAgent Python Agent Service — Configuration."""
import os
from pydantic_settings import BaseSettings
from functools import lru_cache
from dotenv import load_dotenv

# Force loading .env into os.environ so libraries like LiteLLM see new keys on hot-reload
load_dotenv(override=True)

class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Service
    APP_NAME: str = "CoAgent Python Agent"
    APP_VERSION: str = "0.1.0"
    DEBUG: bool = False
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # Java Bridge
    JAVA_API_URL: str = "http://localhost:8080/api/internal"
    JAVA_API_TIMEOUT: int = 30  # seconds

    # Redis (Agent Memory)
    REDIS_URL: str = "redis://localhost:6379"
    MEMORY_MAX_EXCHANGES: int = 20
    MEMORY_TTL_SECONDS: int = 604800  # 7 days

    # LLM (model-agnostic via LiteLLM)
    # Set whichever provider keys you want to use:
    #   OPENAI_API_KEY, ANTHROPIC_API_KEY, GROQ_API_KEY, GEMINI_API_KEY, etc.
    # LiteLLM picks them up automatically from env.
    DEFAULT_LLM_MODEL: str = "groq/meta-llama/llama-4-scout-17b-16e-instruct"
    PLANNING_LLM_MODEL: str = "groq/meta-llama/llama-4-scout-17b-16e-instruct"
    SENSITIVE_LLM_MODEL: str = "ollama/llama3.2"  # local model for PII tasks

    # Privacy
    PII_DETECTION_ENABLED: bool = True
    DEFAULT_SHARING_LEVEL: str = "MINIMAL"  # MINIMAL, STANDARD, DETAILED, FULL

    # Google Calendar + Tasks (Direct API via OAuth 2.0)
    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""
    GOOGLE_REDIRECT_URI: str = "http://localhost:8000/oauth2/callback"
    GOOGLE_SCOPES: str = "https://www.googleapis.com/auth/calendar https://www.googleapis.com/auth/tasks"

    # MCP
    MCP_TRANSPORT: str = "in-process"  # "in-process" or "http"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8", "extra": "ignore"}


@lru_cache
def get_settings() -> Settings:
    return Settings()
