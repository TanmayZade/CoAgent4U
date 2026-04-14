"""Shared DTOs for Java ↔ Python bridge communication."""
from pydantic import BaseModel
from datetime import datetime


# ── Requests (Python → Java) ──


class NotifyRequest(BaseModel):
    """Send a Slack message via Java's existing NotificationPort."""
    user_id: str
    message: str
    blocks: dict | None = None


class CreateApprovalRequest(BaseModel):
    """Create an approval via Java's existing ApprovalModule."""
    agent_id: str
    description: str
    duration_hours: int = 12


class CreateEventRequest(BaseModel):
    """Create a calendar event via Java's existing CalendarPort."""
    title: str
    start: str  # ISO-8601
    end: str  # ISO-8601


class AuditLogRequest(BaseModel):
    """Write an audit/transparency entry via Java's monitoring module."""
    agent_id: str
    action: str
    details: dict = {}


class InitiateCoordinationRequest(BaseModel):
    """Trigger the Java coordination state machine."""
    requester_agent_id: str
    invitee_agent_id: str
    date: str  # YYYY-MM-DD
    duration_minutes: int = 60


# ── Responses (Java → Python) ──


class UserResponse(BaseModel):
    """User data from Java's user-module."""
    user_id: str
    username: str
    slack_user_id: str
    workspace_id: str
    calendar_connected: bool = False


class AgentResponse(BaseModel):
    """Agent data from Java's agent-module."""
    agent_id: str
    user_id: str
    status: str


class CalendarEventResponse(BaseModel):
    """Calendar event from Java's calendar-module."""
    event_id: str
    title: str
    start: str
    end: str


class ApprovalResponse(BaseModel):
    """Approval created via Java."""
    approval_id: str
    status: str


class CoordinationResponse(BaseModel):
    """Coordination initiation result from Java."""
    coordination_id: str
    state: str
    available_slots: list[dict] = []


# ── Agent handle message (Java → Python) ──


class HandleMessageRequest(BaseModel):
    """Incoming message forwarded from Java Slack adapter."""
    agent_id: str
    user_id: str
    raw_text: str
    slack_user_id: str | None = None
    workspace_id: str | None = None
    correlation_id: str | None = None


class HandleMessageResponse(BaseModel):
    """Agent's response back to Java."""
    message: str
    via: str = "python"  # "python-dynamic" or "java-deterministic"
    tools_called: list[str] = []
    pii_detected: list[str] = []
