"""Tests for MCP Calendar Tools — verifies tools work via in-process FastMCP client."""
import json
import pytest
from unittest.mock import AsyncMock

from fastmcp import Client as McpClient

from app.mcp.calendar_tools import calendar_mcp, set_bridge, set_agent_context
from app.bridge.models import CalendarEventResponse


@pytest.fixture(autouse=True)
def setup_agent_context():
    """Set a test agent context for all tests."""
    set_agent_context("test-agent-001")


@pytest.fixture
def mock_bridge():
    """Create a mock JavaBridgeClient and inject it into the MCP module."""
    bridge = AsyncMock()
    set_bridge(bridge)
    return bridge


def _extract_text(result) -> str:
    """Extract text from FastMCP 3.x CallToolResult."""
    if result and result.content:
        return result.content[0].text
    return str(result.data) if result and result.data else ""


class TestGetUpcomingEvents:
    """Test the get_upcoming_events MCP tool."""

    @pytest.mark.asyncio
    async def test_returns_events(self, mock_bridge):
        """When Java has events, tool returns them as JSON."""
        mock_bridge.get_events.return_value = [
            CalendarEventResponse(
                event_id="evt-001",
                title="Team Standup",
                start="2026-04-07T10:00:00Z",
                end="2026-04-07T10:30:00Z",
            ),
            CalendarEventResponse(
                event_id="evt-002",
                title="Lunch with Raj",
                start="2026-04-07T12:00:00Z",
                end="2026-04-07T13:00:00Z",
            ),
        ]

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "get_upcoming_events",
                {"start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        data = json.loads(_extract_text(result))
        assert data["status"] == "ok"
        assert data["count"] == 2
        assert data["events"][0]["title"] == "Team Standup"
        assert data["events"][1]["title"] == "Lunch with Raj"

    @pytest.mark.asyncio
    async def test_empty_calendar(self, mock_bridge):
        """When no events exist, tool returns count=0."""
        mock_bridge.get_events.return_value = []

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "get_upcoming_events",
                {"start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        data = json.loads(_extract_text(result))
        assert data["status"] == "ok"
        assert data["count"] == 0
        assert "No events found" in data["message"]


class TestFindFreeSlots:
    """Test the find_free_slots MCP tool."""

    @pytest.mark.asyncio
    async def test_returns_busy_slots(self, mock_bridge):
        """When Java reports busy slots, tool returns them."""
        mock_bridge.get_freebusy.return_value = [
            {"start": "2026-04-07T10:00:00Z", "end": "2026-04-07T10:30:00Z"},
            {"start": "2026-04-07T14:00:00Z", "end": "2026-04-07T15:00:00Z"},
        ]

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "find_free_slots",
                {"start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        data = json.loads(_extract_text(result))
        assert data["status"] == "ok"
        assert data["busy_slot_count"] == 2

    @pytest.mark.asyncio
    async def test_completely_free(self, mock_bridge):
        """When no busy slots, tool says user is free."""
        mock_bridge.get_freebusy.return_value = []

        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "find_free_slots",
                {"start_date": "2026-04-07", "end_date": "2026-04-08"},
            )

        data = json.loads(_extract_text(result))
        assert data["status"] == "ok"
        assert "completely free" in data["message"]


class TestCreateCalendarEvent:
    """Test the create_calendar_event MCP tool — returns approval proposal."""

    @pytest.mark.asyncio
    async def test_returns_approval_proposal(self, mock_bridge):
        """Create event should NOT actually create — returns pending_approval."""
        async with McpClient(calendar_mcp) as client:
            result = await client.call_tool(
                "create_calendar_event",
                {
                    "title": "Team Sync",
                    "start": "2026-04-07T14:00:00Z",
                    "end": "2026-04-07T15:00:00Z",
                },
            )

        data = json.loads(_extract_text(result))
        assert data["status"] == "pending_approval"
        assert "confirm" in data["message"].lower()
        assert data["proposed_event"]["title"] == "Team Sync"

        # Verify bridge.create_event was NOT called
        mock_bridge.create_event.assert_not_called()
