"""Tests for the Java ↔ Python bridge client."""
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
import httpx

from app.bridge.java_client import JavaBridgeClient
from app.bridge.models import UserResponse, CalendarEventResponse


@pytest.fixture
def bridge():
    """Create a bridge client for testing."""
    with patch("app.bridge.java_client.get_settings") as mock_settings:
        mock_settings.return_value = MagicMock(
            JAVA_API_URL="http://localhost:8080/api/internal",
            JAVA_API_TIMEOUT=5,
        )
        client = JavaBridgeClient()
        yield client


class TestJavaBridgeClient:
    """Tests for JavaBridgeClient — verifies the Python→Java HTTP contract."""

    @pytest.mark.asyncio
    async def test_get_user_success(self, bridge):
        """Test: Python can fetch user data from Java."""
        mock_response = httpx.Response(
            200,
            json={
                "user_id": "123e4567-e89b-12d3-a456-426614174000",
                "username": "tanmay",
                "slack_user_id": "U12345",
                "workspace_id": "T67890",
                "calendar_connected": True,
            },
            request=httpx.Request("GET", "http://test/user/123"),
        )

        bridge.client = AsyncMock()
        bridge.client.get = AsyncMock(return_value=mock_response)

        user = await bridge.get_user("123e4567-e89b-12d3-a456-426614174000")
        assert isinstance(user, UserResponse)
        assert user.username == "tanmay"
        assert user.calendar_connected is True

    @pytest.mark.asyncio
    async def test_get_events_success(self, bridge):
        """Test: Python can fetch calendar events from Java."""
        mock_response = httpx.Response(
            200,
            json=[
                {
                    "event_id": "evt-001",
                    "title": "Standup",
                    "start": "2026-04-05T10:00:00Z",
                    "end": "2026-04-05T10:30:00Z",
                },
                {
                    "event_id": "evt-002",
                    "title": "Design Review",
                    "start": "2026-04-05T14:00:00Z",
                    "end": "2026-04-05T15:00:00Z",
                },
            ],
            request=httpx.Request("GET", "http://test/calendar/agent-1/events"),
        )

        bridge.client = AsyncMock()
        bridge.client.get = AsyncMock(return_value=mock_response)

        events = await bridge.get_events("agent-1", "2026-04-05", "2026-04-05")
        assert len(events) == 2
        assert events[0].title == "Standup"
        assert events[1].title == "Design Review"

    @pytest.mark.asyncio
    async def test_send_slack_message(self, bridge):
        """Test: Python can send Slack messages via Java."""
        mock_response = httpx.Response(
            200,
            json={"status": "sent"},
            request=httpx.Request("POST", "http://test/notify"),
        )

        bridge.client = AsyncMock()
        bridge.client.post = AsyncMock(return_value=mock_response)

        result = await bridge.send_slack_message(
            user_id="user-123",
            message="Hello from Python agent!"
        )
        assert result["status"] == "sent"

    @pytest.mark.asyncio
    async def test_get_user_not_found(self, bridge):
        """Test: Handles 404 from Java gracefully."""
        mock_response = httpx.Response(
            404,
            json={"error": "User not found"},
            request=httpx.Request("GET", "http://test/user/nonexistent"),
        )
        mock_response.raise_for_status = MagicMock(
            side_effect=httpx.HTTPStatusError("Not Found", request=mock_response.request, response=mock_response)
        )

        bridge.client = AsyncMock()
        bridge.client.get = AsyncMock(return_value=mock_response)

        agent = await bridge.get_agent_by_user("nonexistent-user")
        assert agent is None

    @pytest.mark.asyncio
    async def test_log_activity(self, bridge):
        """Test: Python can write audit logs to Java."""
        mock_response = httpx.Response(
            200,
            json={"status": "logged"},
            request=httpx.Request("POST", "http://test/audit/log"),
        )

        bridge.client = AsyncMock()
        bridge.client.post = AsyncMock(return_value=mock_response)

        # Should not raise
        await bridge.log_activity(
            agent_id="agent-1",
            action="tool_execution",
            details={"tools_called": ["list_events"], "pii_detected": []}
        )
