"""LLM Planner — model-agnostic tool-calling LLM via LiteLLM.

Responsible for:
  1. Converting MCP tool schemas into OpenAI-compatible function definitions
  2. Sending user messages + tool definitions to the LLM
  3. Parsing tool_calls from the LLM response
  4. Feeding tool results back for a final natural-language summary
"""
import json
import logging
from dataclasses import dataclass, field

import litellm

from app.config import get_settings

logger = logging.getLogger(__name__)

# Suppress LiteLLM's verbose logging
litellm.suppress_debug_info = True


@dataclass
class ToolCallRequest:
    """A tool call requested by the LLM."""
    id: str
    name: str
    arguments: dict


@dataclass
class PlanResult:
    """Result of an LLM planning step."""
    # If the LLM wants to call tools
    tool_calls: list[ToolCallRequest] = field(default_factory=list)
    # If the LLM responds directly (no tools needed)
    direct_response: str | None = None

    @property
    def has_tool_calls(self) -> bool:
        return len(self.tool_calls) > 0


SYSTEM_PROMPT_TEMPLATE = """You are CoAgent, a helpful, privacy-conscious personal AI assistant.

You help users manage their schedule, calendar, tasks, and productivity.
You have access to 32 tools across three categories.

Important Context:
- Current local datetime: {current_datetime}
- Agent ID for tool calls: {agent_id}

═══ CALENDAR TOOLS (17) ═══
Event management:
- get_upcoming_events: List events for a date range
- search_events: Full-text search across calendars
- get_event_details: Get full details of an event by ID
- create_event: Create event with location, description, reminders, recurrence, color, Google Meet link
- update_event: Modify any field of an existing event
- delete_event: Delete an event
- quick_add_event: Create event from natural language (e.g., "Dentist Friday 3pm")
- move_event: Move event between calendars

Availability:
- find_free_slots: Find when the user is free/busy
- get_recurring_event_instances: List occurrences of a recurring event

Calendar management:
- list_calendars: Show all user's calendars
- create_calendar: Create a secondary calendar
- delete_calendar: Delete a secondary calendar
- clear_calendar: Remove all events from a calendar (dangerous!)

Auth & settings:
- get_auth_status: Check if Google Calendar is connected
- get_auth_url: Get OAuth login URL
- get_calendar_settings: View user's timezone, date format, etc.

═══ TASK TOOLS (8) ═══
- list_task_lists: Show all task lists
- create_task_list: Create a new task list
- list_tasks: List tasks with filters (pending, completed, by due date)
- create_task: Create a task with due date, notes; supports subtasks via parent_task_id
- update_task: Edit task title, notes, due date
- complete_task: Mark a task as done
- delete_task: Delete a task
- clear_completed_tasks: Bulk-remove completed tasks

═══ PRODUCTIVITY TOOLS (7) ═══
- daily_briefing: Morning summary of events + overdue tasks + free time
- weekly_summary: Week stats — meeting hours, busiest day, tasks completed
- time_block_focus: Auto-find free slot and create a focus time block
- schedule_with_preferences: Smart scheduling with constraints (prefer morning, avoid Fridays, etc.)
- set_event_reminders: Set custom popup reminders on an event
- create_recurring_routine: Create recurring habits (e.g., "Gym Mon/Wed/Fri 6am")
- get_agenda: Day's timeline with events and tasks interleaved

Guidelines:
- Be concise and friendly.
- ALWAYS pass agent_id="{agent_id}" as the first argument to every tool call.
- Dates MUST be YYYY-MM-DD format (never words like "Friday"). Times must be ISO-8601.
- When showing events/tasks, format them clearly with dates and times.
- For "add to my to-do" → use create_task. For "what should I do today" → use daily_briefing or get_agenda.
- For "find me time for X" → use schedule_with_preferences or time_block_focus.
- For "set up a routine" → use create_recurring_routine.
- If the user's Google Calendar isn't connected, use get_auth_url to help them connect.
- If the user's request is ambiguous, ask for clarification.
- Never share the user's calendar or task data externally.
- CRUCIAL: You MUST USE the provided standard JSON tool calling mechanism. DO NOT emit raw `<function>` tags as plain text.
"""


def mcp_tools_to_openai_functions(mcp_tools: list[dict]) -> list[dict]:
    """Convert FastMCP tool schemas to OpenAI-compatible function tool definitions.

    FastMCP provides tool schemas via `list_tools()`. This converts them
    to the `tools` format expected by OpenAI / LiteLLM.
    """
    openai_tools = []

    for tool in mcp_tools:
        # FastMCP tool schema has: name, description, inputSchema
        func_def = {
            "type": "function",
            "function": {
                "name": tool.get("name", ""),
                "description": tool.get("description", ""),
                "parameters": tool.get("inputSchema", {"type": "object", "properties": {}}),
            },
        }
        openai_tools.append(func_def)

    return openai_tools


class LlmPlanner:
    """Plans tool calls using an LLM via LiteLLM (model-agnostic)."""

    def __init__(self, model: str | None = None):
        settings = get_settings()
        self.model = model or settings.DEFAULT_LLM_MODEL
        logger.info(f"[LLM] Planner initialized with model={self.model}")

    async def plan(
        self,
        user_message: str,
        mcp_tools: list[dict],
        conversation_history: list[dict] | None = None,
        agent_id: str = "default",
    ) -> PlanResult:
        """Ask the LLM to plan a response, possibly requesting tool calls.

        Args:
            user_message: The user's raw text
            mcp_tools: MCP tool schemas (from FastMCP list_tools)
            conversation_history: Optional prior messages for context
            agent_id: The agent ID to inject into the system prompt

        Returns:
            PlanResult with either tool_calls or a direct_response
        """
        # Build messages
        from datetime import datetime
        # If your environment doesn't have local timezone set right, you might need to rely on the OS
        now_str = datetime.now().astimezone().isoformat()
        system_prompt = SYSTEM_PROMPT_TEMPLATE.format(
            current_datetime=now_str,
            agent_id=agent_id,
        )
        messages = [{"role": "system", "content": system_prompt}]

        if conversation_history:
            messages.extend(conversation_history)

        messages.append({"role": "user", "content": user_message})

        # Convert MCP tools to OpenAI format
        tools = mcp_tools_to_openai_functions(mcp_tools) if mcp_tools else None

        logger.info(f"[LLM] Planning with {len(mcp_tools)} tools, model={self.model}")

        try:
            response = await litellm.acompletion(
                model=self.model,
                messages=messages,
                tools=tools,
                tool_choice="auto",
                temperature=0.1,  # Low temp for reliable tool selection
            )

            choice = response.choices[0]
            message = choice.message

            # Check for tool calls
            if message.tool_calls:
                tool_calls = []
                for tc in message.tool_calls:
                    try:
                        args = json.loads(tc.function.arguments)
                    except json.JSONDecodeError:
                        args = {}

                    tool_calls.append(ToolCallRequest(
                        id=tc.id,
                        name=tc.function.name,
                        arguments=args,
                    ))

                logger.info(f"[LLM] Planned {len(tool_calls)} tool call(s): "
                            f"{[tc.name for tc in tool_calls]}")
                for tc in tool_calls:
                    logger.info(f"[LLM] Tool arguments generated for {tc.name}: {tc.arguments}")
                return PlanResult(tool_calls=tool_calls)

            # Fallback for models outputting raw <function=name>args</function> XML strings
            if message.content and "<function=" in message.content:
                import re
                import uuid
                tool_calls = []
                matches = re.finditer(r'<function=(\w+)>(.*?)</function>', message.content, re.DOTALL)
                for match in matches:
                    tool_name = match.group(1)
                    kwargs_str = match.group(2).strip()
                    args = {}
                    if kwargs_str.startswith("{") and kwargs_str.endswith("}"):
                        try:
                            args = json.loads(kwargs_str)
                        except json.JSONDecodeError:
                            pass
                    else:
                        kw_matches = re.finditer(r'(\w+)\s*=\s*("[^"]*"|\'[^\']*\'|[^,\s]+)', kwargs_str)
                        for kw in kw_matches:
                            k = kw.group(1)
                            v = kw.group(2)
                            if (v.startswith('"') and v.endswith('"')) or (v.startswith("'") and v.endswith("'")):
                                v = v[1:-1]
                            args[k] = v

                    if tool_name:
                        tool_calls.append(ToolCallRequest(
                            id=f"call_{uuid.uuid4().hex[:10]}",
                            name=tool_name,
                            arguments=args,
                        ))
                
                if tool_calls:
                    logger.info(f"[LLM] Parsed {len(tool_calls)} raw XML tool call(s) from content")
                    return PlanResult(tool_calls=tool_calls)

            # No tool calls — direct response
            direct = message.content or "I'm not sure how to help with that."
            logger.info(f"[LLM] Direct response (no tools needed)")
            return PlanResult(direct_response=direct)

        except Exception as e:
            error_str = str(e)

            # Groq-specific: tool_use_failed — retry WITHOUT conversation history 
            # (old history confuses Groq's tool call format)
            if "tool_use_failed" in error_str:
                logger.warning("[LLM] Groq tool_use_failed — retrying with clean context")
                try:
                    clean_messages = [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_message},
                    ]
                    response = await litellm.acompletion(
                        model=self.model,
                        messages=clean_messages,
                        tools=tools,
                        tool_choice="auto",
                        temperature=0.1,
                    )
                    choice = response.choices[0]
                    message = choice.message

                    if message.tool_calls:
                        tool_calls = []
                        for tc in message.tool_calls:
                            try:
                                args = json.loads(tc.function.arguments)
                            except json.JSONDecodeError:
                                args = {}
                            tool_calls.append(ToolCallRequest(
                                id=tc.id,
                                name=tc.function.name,
                                arguments=args,
                            ))
                        logger.info(f"[LLM] Retry succeeded with {len(tool_calls)} tool call(s)")
                        return PlanResult(tool_calls=tool_calls)

                    direct = message.content or "I'm not sure how to help with that."
                    return PlanResult(direct_response=direct)

                except Exception as retry_e:
                    logger.error(f"[LLM] Retry also failed: {retry_e}")

            logger.error(f"[LLM] Planning failed: {e}", exc_info=True)
            return PlanResult(
                direct_response=(
                    "I'm having trouble processing your request right now. "
                    "Please try again in a moment."
                )
            )

    async def summarize(
        self,
        user_message: str,
        tool_results: list[dict],
        history: list[dict] | None = None,
    ) -> str:
        """Feed tool results back to the LLM for a final natural-language summary.

        Args:
            user_message: Original user request
            tool_results: List of {tool_call_id, name, result} dicts

        Returns:
            Natural-language response string
        """
        from datetime import datetime
        now_str = datetime.now().astimezone().isoformat()
        system_prompt = SYSTEM_PROMPT_TEMPLATE.format(
            current_datetime=now_str,
            agent_id="system-summary",
        )
        messages = [{"role": "system", "content": system_prompt}]
        
        if history:
            messages.extend(history)
            
        messages.append({"role": "user", "content": user_message})

        # Reconstruct the assistant message with tool calls
        # Then add tool results as tool messages
        assistant_tool_calls = []
        for tr in tool_results:
            assistant_tool_calls.append({
                "id": tr["tool_call_id"],
                "type": "function",
                "function": {
                    "name": tr["name"],
                    "arguments": "{}",  # not needed for summarization
                },
            })

        messages.append({
            "role": "assistant",
            "content": None,
            "tool_calls": assistant_tool_calls,
        })

        for tr in tool_results:
            messages.append({
                "role": "tool",
                "tool_call_id": tr["tool_call_id"],
                "content": tr["result"],
            })

        logger.info(f"[LLM] Summarizing {len(tool_results)} tool result(s)")

        try:
            response = await litellm.acompletion(
                model=self.model,
                messages=messages,
                temperature=0.3,
            )

            return response.choices[0].message.content or "Here are your results."

        except Exception as e:
            logger.error(f"[LLM] Summarize failed: {e}", exc_info=True)
            # Fallback: return raw tool results
            return self._fallback_summary(tool_results)

    def _fallback_summary(self, tool_results: list[dict]) -> str:
        """Generate a basic summary when LLM is unavailable."""
        parts = ["Here's what I found:"]
        for tr in tool_results:
            try:
                data = json.loads(tr["result"])
                if "events" in data:
                    count = data.get("count", len(data["events"]))
                    parts.append(f"\n📅 Found {count} event(s).")
                    for evt in data["events"]:
                        parts.append(f"  • {evt.get('title', 'Untitled')} — {evt.get('start', '?')}")
                elif "message" in data:
                    parts.append(f"\n{data['message']}")
            except (json.JSONDecodeError, KeyError):
                parts.append(f"\n{tr['result']}")

        return "\n".join(parts)
