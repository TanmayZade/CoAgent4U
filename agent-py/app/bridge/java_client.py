"""HTTP client for calling the Java Spring Boot internal API.

This is the bridge between Python (agent intelligence) and Java (system of record).
Java handles: persistence, auth, Slack messaging, approvals, coordination state machine.
Python calls Java for all of these via this client.
"""
import logging
from typing import Any

import httpx

from app.config import get_settings
from app.bridge.models import (
    NotifyRequest,
    CreateApprovalRequest,
    CreateEventRequest,
    AuditLogRequest,
    InitiateCoordinationRequest,
    UserResponse,
    AgentResponse,
    CalendarEventResponse,
    ApprovalResponse,
    CoordinationResponse,
)

logger = logging.getLogger(__name__)


class JavaBridgeClient:
    """Async HTTP client that communicates with the Java Spring Boot backend.

    All methods call the InternalBridgeController on the Java side,
    which delegates to existing ports/adapters (CalendarPort, NotificationPort, etc.).
    """

    def __init__(self):
        settings = get_settings()
        self.base_url = settings.JAVA_API_URL
        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            timeout=httpx.Timeout(settings.JAVA_API_TIMEOUT),
            headers={"X-Internal-Service": "coagent-python"},
        )

    async def close(self):
        await self.client.aclose()

    # ──────────────────────────────────────────────────────────────
    # Calendar (wraps existing GoogleCalendarAdapter)
    # ──────────────────────────────────────────────────────────────

    async def get_events(
        self, agent_id: str, start_date: str, end_date: str
    ) -> list[CalendarEventResponse]:
        """Get calendar events for a date range."""
        resp = await self._get(
            f"/calendar/{agent_id}/events",
            params={"start": start_date, "end": end_date},
        )
        return [CalendarEventResponse(**e) for e in resp]

    async def get_freebusy(
        self, agent_id: str, start_date: str, end_date: str
    ) -> list[dict]:
        """Get free/busy time slots."""
        return await self._get(
            f"/calendar/{agent_id}/freebusy",
            params={"start": start_date, "end": end_date},
        )

    async def create_event(
        self, agent_id: str, title: str, start: str, end: str
    ) -> dict:
        """Create a calendar event."""
        req = CreateEventRequest(title=title, start=start, end=end)
        return await self._post(f"/calendar/{agent_id}/events", data=req.model_dump())

    async def delete_event(self, agent_id: str, event_id: str) -> dict:
        """Delete a calendar event."""
        return await self._delete(f"/calendar/{agent_id}/events/{event_id}")

    # ──────────────────────────────────────────────────────────────
    # Notifications (wraps existing SlackNotificationAdapter)
    # ──────────────────────────────────────────────────────────────

    async def send_slack_message(
        self, user_id: str, message: str, blocks: dict | None = None
    ) -> dict:
        """Send a Slack message via Java's existing NotificationPort."""
        req = NotifyRequest(user_id=user_id, message=message, blocks=blocks)
        return await self._post("/notify", data=req.model_dump())

    # ──────────────────────────────────────────────────────────────
    # Approvals (wraps existing ApprovalModule)
    # ──────────────────────────────────────────────────────────────

    async def create_approval(
        self, agent_id: str, description: str, duration_hours: int = 12
    ) -> ApprovalResponse:
        """Create an approval request via Java's approval-module."""
        req = CreateApprovalRequest(
            agent_id=agent_id, description=description, duration_hours=duration_hours
        )
        resp = await self._post("/approval/create", data=req.model_dump())
        return ApprovalResponse(**resp)

    # ──────────────────────────────────────────────────────────────
    # Users (wraps existing UserPersistencePort)
    # ──────────────────────────────────────────────────────────────

    async def get_user(self, user_id: str) -> UserResponse:
        """Look up user info — Slack ID, workspace, connection status."""
        resp = await self._get(f"/user/{user_id}")
        return UserResponse(**resp)

    async def get_agent_by_user(self, user_id: str) -> AgentResponse | None:
        """Resolve user → agent mapping."""
        try:
            resp = await self._get(f"/agent/by-user/{user_id}")
            return AgentResponse(**resp)
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                return None
            raise

    # ──────────────────────────────────────────────────────────────
    # Audit / Transparency (wraps existing monitoring module)
    # ──────────────────────────────────────────────────────────────

    async def log_activity(self, agent_id: str, action: str, details: dict) -> None:
        """Write a transparency/audit entry."""
        req = AuditLogRequest(agent_id=agent_id, action=action, details=details)
        await self._post("/audit/log", data=req.model_dump())

    # ──────────────────────────────────────────────────────────────
    # Coordination (wraps existing CoordinationProtocolPort)
    # ──────────────────────────────────────────────────────────────

    async def initiate_coordination(
        self,
        requester_agent_id: str,
        invitee_agent_id: str,
        date: str,
        duration_minutes: int = 60,
    ) -> CoordinationResponse:
        """Trigger the Java coordination state machine."""
        req = InitiateCoordinationRequest(
            requester_agent_id=requester_agent_id,
            invitee_agent_id=invitee_agent_id,
            date=date,
            duration_minutes=duration_minutes,
        )
        resp = await self._post("/coordination/initiate", data=req.model_dump())
        return CoordinationResponse(**resp)

    # ──────────────────────────────────────────────────────────────
    # Passthrough (for deterministic intents handled by Java)
    # ──────────────────────────────────────────────────────────────

    async def passthrough_to_java(
        self, agent_id: str, user_id: str, raw_text: str, intent_type: str
    ) -> str:
        """Forward a deterministic intent back to Java for handling.

        Java's AgentCommandService handles ADD_EVENT, VIEW_SCHEDULE, SCHEDULE_WITH
        via the existing state machine. No Python involvement needed.
        """
        resp = await self._post(
            "/agent/passthrough",
            data={
                "agentId": agent_id,
                "userId": user_id,
                "rawText": raw_text,
                "intentType": intent_type,
            },
        )
        return resp.get("message", "Done")

    # ──────────────────────────────────────────────────────────────
    # Contacts (for CAP consent engine — Phase 4)
    # ──────────────────────────────────────────────────────────────

    async def get_contact(self, owner_id: str, contact_id: str) -> dict | None:
        """Get contact relationship and trust level."""
        try:
            return await self._get(f"/contacts/{owner_id}/{contact_id}")
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                return None
            raise

    # ──────────────────────────────────────────────────────────────
    # Token Persistence (Delegates to Java DB and AES Encryption)
    # ──────────────────────────────────────────────────────────────

    async def get_token(self, agent_id: str) -> dict | None:
        """Get the full Google OAuth token JSON from the database."""
        try:
            resp = await self._get(f"/agent/{agent_id}/google-token")
            token_json_str = resp.get("token_json")
            if token_json_str:
                import json
                try:
                    return json.loads(token_json_str)
                except json.JSONDecodeError:
                    logger.warning(
                        f"[Bridge] Token for agent={agent_id} is not valid JSON "
                        f"(len={len(token_json_str)}). Re-authorization may be needed."
                    )
                    return None
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                return None
            raise
        return None

    async def save_token(self, agent_id: str, token_data: dict) -> None:
        """Save the full Google OAuth token JSON to the database."""
        import json
        await self._post(
            f"/agent/{agent_id}/google-token",
            data={"token_json": json.dumps(token_data)},
        )

    async def delete_token(self, agent_id: str) -> None:
        """Delete the Google OAuth token from the database."""
        try:
            await self._delete(f"/agent/{agent_id}/google-token")
        except httpx.HTTPStatusError as e:
            if e.response.status_code != 404:
                raise

    async def get_contact(self, owner_id: str, contact_id: str) -> dict | None:
        """Get contact relationship and trust level."""
        try:
            return await self._get(f"/contacts/{owner_id}/{contact_id}")
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                return None
            raise

    # ──────────────────────────────────────────────────────────────
    # Internal HTTP helpers
    # ──────────────────────────────────────────────────────────────

    async def _get(self, path: str, params: dict | None = None) -> Any:
        logger.debug(f"[Bridge] GET {path} params={params}")
        resp = await self.client.get(path, params=params)
        resp.raise_for_status()
        return resp.json()

    async def _post(self, path: str, data: dict | None = None) -> Any:
        logger.debug(f"[Bridge] POST {path}")
        resp = await self.client.post(path, json=data)
        resp.raise_for_status()
        return resp.json()

    async def _delete(self, path: str) -> Any:
        logger.debug(f"[Bridge] DELETE {path}")
        resp = await self.client.delete(path)
        resp.raise_for_status()
        return resp.json()
