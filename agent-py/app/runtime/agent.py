"""Agent Runtime — orchestrates the plan → execute → respond cycle.

Connects 3 MCP tool servers (calendar, tasks, productivity) to the LLM planner.
All tools talk directly to Google Calendar/Tasks API via OAuth.

Flow:
  1. Receive user message
  2. Gather tool schemas from all 3 MCP servers
  3. LLM plans (may request tool calls)
  4. Execute MCP tools via in-process clients
  5. LLM summarizes tool results into natural language
  6. Return response
"""
import json
import logging
from dataclasses import dataclass

from fastmcp import Client as McpClient

from app.llm.planner import LlmPlanner
from app.bridge.models import HandleMessageRequest, HandleMessageResponse
from app.mcp.calendar_tools import calendar_mcp
from app.mcp.task_tools import task_mcp
from app.mcp.productivity_tools import productivity_mcp
from app.runtime.memory import ConversationMemory

logger = logging.getLogger(__name__)


@dataclass
class ToolExecResult:
    """Result from executing a single MCP tool."""
    tool_call_id: str
    name: str
    result: str
    success: bool


class AgentRuntime:
    """Agent runtime that connects LLM planner to all MCP tool servers.

    Uses 3 in-process MCP clients:
      - calendar_mcp: 17 calendar tools
      - task_mcp: 8 task tools
      - productivity_mcp: 7 productivity tools
    """

    def __init__(self, planner: LlmPlanner, memory: ConversationMemory):
        self.planner = planner
        self.memory = memory

        # Create MCP clients for each tool server
        self.mcp_clients = {
            "calendar": McpClient(calendar_mcp),
            "tasks": McpClient(task_mcp),
            "productivity": McpClient(productivity_mcp),
        }
        logger.info("[Runtime] AgentRuntime initialized with 3 MCP tool servers")

    async def _gather_all_tools(self) -> tuple[list[dict], dict[str, McpClient]]:
        """Gather tool schemas from all MCP servers and build a tool→client map.

        Returns:
            (mcp_tool_defs, tool_client_map) where tool_client_map maps tool_name → client
        """
        mcp_tool_defs = []
        tool_client_map: dict[str, McpClient] = {}

        for server_name, client in self.mcp_clients.items():
            async with client as c:
                # We can't use the client outside the async context,
                # so we just collect the tool definitions here
                tool_list = await c.list_tools()
                for tool in tool_list:
                    mcp_tool_defs.append({
                        "name": tool.name,
                        "description": tool.description or "",
                        "inputSchema": tool.inputSchema if hasattr(tool, 'inputSchema') else {},
                    })
                    tool_client_map[tool.name] = server_name

        return mcp_tool_defs, tool_client_map

    def _resolve_server(self, tool_name: str) -> str:
        """Determine which MCP server a tool belongs to based on naming convention."""
        # Task tools
        task_tools = {
            "list_task_lists", "create_task_list", "list_tasks",
            "create_task", "update_task", "complete_task",
            "delete_task", "clear_completed_tasks",
        }
        # Productivity tools
        prod_tools = {
            "daily_briefing", "weekly_summary", "time_block_focus",
            "schedule_with_preferences", "set_event_reminders",
            "create_recurring_routine", "get_agenda",
        }

        if tool_name in task_tools:
            return "tasks"
        elif tool_name in prod_tools:
            return "productivity"
        else:
            return "calendar"

    async def handle(self, request: HandleMessageRequest) -> HandleMessageResponse:
        """Handle a user message end-to-end."""
        logger.info(
            f"[Runtime] Handling message: agent={request.agent_id} "
            f"text='{request.raw_text[:80]}'"
        )

        tools_called = []

        try:
            # ── Step 1: Gather all tool schemas ──
            all_tool_defs = []
            tool_server_map = {}

            for server_name, client in self.mcp_clients.items():
                async with client as c:
                    tool_list = await c.list_tools()
                    for tool in tool_list:
                        all_tool_defs.append({
                            "name": tool.name,
                            "description": tool.description or "",
                            "inputSchema": tool.inputSchema if hasattr(tool, 'inputSchema') else {},
                        })
                        tool_server_map[tool.name] = server_name

            logger.info(f"[Runtime] Gathered {len(all_tool_defs)} tools from {len(self.mcp_clients)} servers")

            # ── Step 1.5: Retrieve recent context ──
            history = await self.memory.get_history(request.agent_id)

            # ── Step 2: LLM plans ──
            plan = await self.planner.plan(
                user_message=request.raw_text,
                mcp_tools=all_tool_defs,
                conversation_history=history,
                agent_id=request.agent_id,
            )

            # ── Step 3a: Direct response (no tools needed) ──
            if not plan.has_tool_calls:
                logger.info("[Runtime] LLM responded directly (no tools)")
                await self.memory.add_exchange(
                    agent_id=request.agent_id,
                    user_msg=request.raw_text,
                    assistant_msg=plan.direct_response,
                )
                return HandleMessageResponse(
                    message=plan.direct_response,
                    via="python-dynamic",
                    tools_called=[],
                )

            # ── Step 3b: Execute tool calls ──
            tool_results = []
            for tc in plan.tool_calls:
                logger.info(f"[Runtime] Executing tool: {tc.name}({tc.arguments})")
                tools_called.append(tc.name)

                # Inject agent_id into tool arguments if not already present
                if "agent_id" not in tc.arguments:
                    tc.arguments["agent_id"] = request.agent_id

                # Determine which MCP server to use
                server_name = self._resolve_server(tc.name)
                mcp_server = {
                    "calendar": calendar_mcp,
                    "tasks": task_mcp,
                    "productivity": productivity_mcp,
                }[server_name]

                try:
                    async with McpClient(mcp_server) as client:
                        result = await client.call_tool(tc.name, tc.arguments)

                    result_text = ""
                    if result and result.content:
                        for content_item in result.content:
                            if hasattr(content_item, 'text'):
                                result_text += content_item.text

                    if not result_text and result:
                        result_text = str(result.data) if result.data else ""

                    tool_results.append(ToolExecResult(
                        tool_call_id=tc.id,
                        name=tc.name,
                        result=result_text,
                        success=not (result and result.is_error),
                    ))
                    logger.info(f"[Runtime] Tool {tc.name} succeeded via {server_name}")

                except Exception as e:
                    logger.error(f"[Runtime] Tool {tc.name} failed: {e}")
                    tool_results.append(ToolExecResult(
                        tool_call_id=tc.id,
                        name=tc.name,
                        result=json.dumps({"error": str(e)}),
                        success=False,
                    ))

            # ── Step 4: LLM summarizes tool results ──
            summary = await self.planner.summarize(
                user_message=request.raw_text,
                tool_results=[
                    {
                        "tool_call_id": tr.tool_call_id,
                        "name": tr.name,
                        "result": tr.result,
                    }
                    for tr in tool_results
                ],
                history=history,
            )

            await self.memory.add_exchange(
                agent_id=request.agent_id,
                user_msg=request.raw_text,
                assistant_msg=summary,
            )

            return HandleMessageResponse(
                message=summary,
                via="python-dynamic",
                tools_called=tools_called,
            )

        except Exception as e:
            logger.error(f"[Runtime] Handle failed: {e}", exc_info=True)
            return HandleMessageResponse(
                message=(
                    "I ran into an issue processing your request. "
                    "Please try again or use one of these commands:\n"
                    "• `show my schedule`\n"
                    "• `add event <title> on <date> at <time>`\n"
                    "• `show my tasks`\n"
                    "• `give me my morning briefing`"
                ),
                via="python-error",
                tools_called=tools_called,
            )
