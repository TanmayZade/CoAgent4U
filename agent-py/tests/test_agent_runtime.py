"""Tests for Agent Runtime — verifies the plan → execute → respond cycle."""
import pytest
from unittest.mock import AsyncMock

from app.runtime.agent import AgentRuntime
from app.runtime.memory import ConversationMemory
from app.llm.planner import LlmPlanner, PlanResult
from app.bridge.models import HandleMessageRequest


class TestAgentRuntime:
    """Tests for the AgentRuntime plan→execute→respond cycle."""

    @pytest.mark.asyncio
    async def test_direct_response_no_tools(self):
        """When LLM responds directly, no tools are called."""
        planner = AsyncMock(spec=LlmPlanner)
        planner.plan.return_value = PlanResult(
            direct_response="Hello! How can I help you today?"
        )
        memory = AsyncMock(spec=ConversationMemory)

        runtime = AgentRuntime(planner=planner, memory=memory)
        request = HandleMessageRequest(
            agent_id="test-agent-001",
            user_id="test-user-001",
            raw_text="hello",
        )

        response = await runtime.handle(request)

        assert response.via == "python-dynamic"
        assert "Hello" in response.message
        assert response.tools_called == []

    @pytest.mark.asyncio
    async def test_error_handling(self):
        """When planning fails, runtime returns a graceful error."""
        planner = AsyncMock(spec=LlmPlanner)
        planner.plan.side_effect = Exception("LLM unavailable")
        memory = AsyncMock(spec=ConversationMemory)

        runtime = AgentRuntime(planner=planner, memory=memory)
        request = HandleMessageRequest(
            agent_id="test-agent-001",
            user_id="test-user-001",
            raw_text="what's on my schedule?",
        )

        response = await runtime.handle(request)

        assert response.via == "python-error"
        assert "issue" in response.message.lower() or "try again" in response.message.lower()
