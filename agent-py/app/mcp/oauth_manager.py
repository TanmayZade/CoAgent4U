"""OAuth 2.0 Manager — handles Google OAuth token lifecycle per agent.

Responsibilities:
  - Generate authorization URLs for the consent screen
  - Exchange authorization codes for access/refresh tokens
  - Persist tokens to the database via the Java bridge
  - Auto-refresh expired tokens
  - Revoke access
"""
import json
import logging
import os

from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from google.auth.transport.requests import Request as GoogleAuthRequest

# Prevent oauthlib from throwing errors when Google returns slightly different sub-scopes than requested
os.environ["OAUTHLIB_RELAX_TOKEN_SCOPE"] = "1"

from app.config import get_settings
from app.bridge.java_client import JavaBridgeClient

logger = logging.getLogger(__name__)

# ── Constants ─────────────────────────────────────────────────
CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"
ALL_SCOPES = [CALENDAR_SCOPE, TASKS_SCOPE]


class OAuthManager:
    """Manages per-agent Google OAuth 2.0 credentials."""

    def __init__(self):
        settings = get_settings()
        self.client_id = settings.GOOGLE_CLIENT_ID
        self.client_secret = settings.GOOGLE_CLIENT_SECRET
        self.redirect_uri = settings.GOOGLE_REDIRECT_URI
        self.scopes = settings.GOOGLE_SCOPES.split()


        # Build client config dict (replaces credentials.json file)
        self._client_config = {
            "web": {
                "client_id": self.client_id,
                "client_secret": self.client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [self.redirect_uri],
            }
        }
        
        # Keep track of pending OAuth flows in memory to preserve the PKCE code_verifier
        self._pending_flows: dict[str, Flow] = {}
        
        self.java_client = JavaBridgeClient()
        logger.info(f"[OAuth] Manager initialized (using Java DB for token persistence)")

    # ── Authorization URL ─────────────────────────────────────

    def get_auth_url(self, agent_id: str) -> str:
        """Generate a Google OAuth consent URL.

        The agent_id is passed as `state` so we can map the callback
        back to the correct agent.
        """
        flow = Flow.from_client_config(
            self._client_config,
            scopes=self.scopes,
            redirect_uri=self.redirect_uri,
        )
        auth_url, _ = flow.authorization_url(
            access_type="offline",       # Get refresh token
            include_granted_scopes="true",
            prompt="consent",            # Always show consent (so we get refresh token)
            state=agent_id,              # Pass agent_id through OAuth state
        )
        # Save the flow instance to preserve the PKCE code_verifier
        self._pending_flows[agent_id] = flow
        logger.info(f"[OAuth] Auth URL generated for agent={agent_id}")
        return auth_url

    # ── Handle Callback ───────────────────────────────────────

    async def handle_callback(self, agent_id: str, auth_code: str) -> Credentials:
        """Exchange an authorization code for tokens and persist them.

        Args:
            agent_id: The agent to store tokens for
            auth_code: The authorization code from Google's callback

        Returns:
            google.oauth2.credentials.Credentials object
        """
        # Retrieve the pending flow to keep the code_verifier, or create a new one as fallback
        flow = self._pending_flows.pop(agent_id, None)
        if not flow:
            logger.warning(f"[OAuth] No pending flow found for agent={agent_id}, falling back to new flow")
            flow = Flow.from_client_config(
                self._client_config,
                scopes=self.scopes,
                redirect_uri=self.redirect_uri,
            )
            
        flow.fetch_token(code=auth_code)
        creds = flow.credentials

        # Persist to DB
        await self._save_credentials(agent_id, creds)
        logger.info(f"[OAuth] Tokens saved for agent={agent_id}")
        return creds

    # ── Get Credentials (with auto-refresh) ───────────────────

    async def get_credentials(self, agent_id: str) -> Credentials | None:
        """Load stored credentials for an agent, auto-refreshing if expired.

        Returns:
            Credentials if available and valid/refreshable, None if not authorized.
        """
        token_data = await self.java_client.get_token(agent_id)
        if not token_data:
            logger.debug(f"[OAuth] No token found in DB for agent={agent_id}")
            return None

        try:
            creds = Credentials.from_authorized_user_info(token_data, self.scopes)
        except Exception as e:
            logger.error(f"[OAuth] Failed to load token for agent={agent_id}: {e}")
            return None

        # Refresh if expired
        if creds and creds.expired and creds.refresh_token:
            try:
                creds.refresh(GoogleAuthRequest())
                await self._save_credentials(agent_id, creds)
                logger.info(f"[OAuth] Token refreshed for agent={agent_id}")
            except Exception as e:
                logger.error(f"[OAuth] Token refresh failed for agent={agent_id}: {e}")
                return None

        if creds and creds.valid:
            return creds

        return None

    # ── Check if authorized ───────────────────────────────────

    async def is_authorized(self, agent_id: str) -> bool:
        """Check if an agent has valid Google credentials."""
        return await self.get_credentials(agent_id) is not None

    # ── Revoke Access ─────────────────────────────────────────

    async def revoke_access(self, agent_id: str) -> bool:
        """Revoke tokens and delete stored credentials from DB.

        Returns:
            True if revoked successfully, False if no credentials found.
        """
        import httpx

        creds = await self.get_credentials(agent_id)
        if creds and creds.token:
            try:
                httpx.post(
                    "https://oauth2.googleapis.com/revoke",
                    params={"token": creds.token},
                    headers={"Content-Type": "application/x-www-form-urlencoded"},
                )
                logger.info(f"[OAuth] Token revoked for agent={agent_id}")
            except Exception as e:
                logger.warning(f"[OAuth] Revocation request failed: {e}")

        # Delete token from DB
        await self.java_client.delete_token(agent_id)
        logger.info(f"[OAuth] Token DB entry deleted for agent={agent_id}")
        return True

    # ── Internal: Save credentials ────────────────────────────

    async def _save_credentials(self, agent_id: str, creds: Credentials) -> None:
        """Serialize credentials and save to DB."""
        token_data = {
            "token": creds.token,
            "refresh_token": creds.refresh_token,
            "token_uri": creds.token_uri,
            "client_id": creds.client_id,
            "client_secret": creds.client_secret,
            "scopes": list(creds.scopes) if creds.scopes else self.scopes,
        }
        if creds.expiry:
            token_data["expiry"] = creds.expiry.isoformat()

        await self.java_client.save_token(agent_id, token_data)
