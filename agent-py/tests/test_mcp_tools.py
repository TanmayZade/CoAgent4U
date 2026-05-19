"""Tests for MCP Calendar Tools — verifies tools work via in-process FastMCP client."""
import json
import pytest
from unittest.mock import AsyncMock, patch

from fastmcp import Client as McpClient

from app.mcp.calendar_tools import calendar_mcp, set_google_client, set_oauth_manager


@pytest.fixture(autouse=True)
def setup_mocks():
    """Inject mock Google client and OAuth manager into calendar_tools."""
    mock_gcal = AsyncMock()
    mock_oauth = AsyncMock()
    set_google_client(mock_gcal)
    set_oauth_manager(mock_oauth)
    return mock_gcal, mock_oauth


class TestGetUpcomingEvents:
    """Test the get_upcoming_events MCP tool."""

    @pytest.mark.asyncio
    async def test_returns_events(self, setup_mocks):
        """When Google Calendar has events, tool returns them as JSON."""
        mock_gcal, mock_oauth = setup_mocks
        mock_oauth.get_credentials.return_value = AsyncMock()  # valid creds

        mock_gcal.list_events.return_value = [
            {
                "id": "evt-001",
                "summary": "Team Standup",
                "start": {"dateTime": "2026-04-07T10:00:00Z"},
                "end": {"dateTime": "2026-04-07T10:30:00Z"},
            },
            {
                "id": "evt-002",
                "summary": "Lunch with Raj",
                "start": {"dateTime": "2026-04-07T12:00:00Z"},
                "end": {"dateTime": "2026-04-07T13:00:00Z"},
            },
        ]

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "get_upcoming_events",
                {"agent_id": "test-agent-001", "start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        text = result.content[0].text if result.content else ""
        data = json.loads(text)
        assert data["status"] == "ok"
        assert data["count"] == 2
        assert data["events"][0]["title"] == "Team Standup"
        assert data["events"][1]["title"] == "Lunch with Raj"

    @pytest.mark.asyncio
    async def test_empty_calendar(self, setup_mocks):
        """When no events exist, tool returns count=0."""
        mock_gcal, mock_oauth = setup_mocks
        mock_oauth.get_credentials.return_value = AsyncMock()
        mock_gcal.list_events.return_value = []

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "get_upcoming_events",
                {"agent_id": "test-agent-001", "start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        text = result.content[0].text if result.content else ""
        data = json.loads(text)
        assert data["status"] == "ok"
        assert data["count"] == 0


class TestFindFreeSlots:
    """Test the find_free_slots MCP tool."""

    @pytest.mark.asyncio
    async def test_returns_busy_slots(self, setup_mocks):
        """When Google reports busy slots, tool returns them."""
        mock_gcal, mock_oauth = setup_mocks
        mock_oauth.get_credentials.return_value = AsyncMock()
        mock_gcal.query_free_busy.return_value = {
            "calendars": {
                "primary": {
                    "busy": [
                        {"start": "2026-04-07T10:00:00Z", "end": "2026-04-07T10:30:00Z"},
                        {"start": "2026-04-07T14:00:00Z", "end": "2026-04-07T15:00:00Z"},
                    ]
                }
            }
        }

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "find_free_slots",
                {"agent_id": "test-agent-001", "start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        text = result.content[0].text if result.content else ""
        data = json.loads(text)
        assert data["status"] == "ok"
        assert data["busy_slot_count"] == 2

    @pytest.mark.asyncio
    async def test_completely_free(self, setup_mocks):
        """When no busy slots, tool says user is free."""
        mock_gcal, mock_oauth = setup_mocks
        mock_oauth.get_credentials.return_value = AsyncMock()
        mock_gcal.query_free_busy.return_value = {"calendars": {"primary": {"busy": []}}}

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "find_free_slots",
                {"agent_id": "test-agent-001", "start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        text = result.content[0].text if result.content else ""
        data = json.loads(text)
        assert data["status"] == "ok"
        assert "completely free" in data["message"]
