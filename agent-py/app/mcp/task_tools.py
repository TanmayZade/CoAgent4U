"""MCP Task Tools — Google Tasks API v1 integration.

8 personal task management tools exposed via MCP.
Full CRUD on task lists and tasks, including subtasks and bulk operations.

Tools:
  list_task_lists, create_task_list,
  list_tasks, create_task, update_task, complete_task,
  delete_task, clear_completed_tasks
"""
import json
import logging
from datetime import date, datetime, timezone

from fastmcp import FastMCP

from app.mcp.google_calendar_client import GoogleCalendarClient
from app.mcp.oauth_manager import OAuthManager

logger = logging.getLogger(__name__)

# ── MCP Server Instance ──────────────────────────────────────

task_mcp = FastMCP(
    "CoAgent Tasks Server",
    client_log_level="DEBUG",
    instructions=(
        "You are a personal task management assistant. "
        "Use these tools to manage the user's Google Tasks — "
        "create to-do lists, add tasks with due dates, mark tasks complete, etc."
    ),
)

# ── Injected at startup ──────────────────────────────────────
_gcal: GoogleCalendarClient | None = None
_oauth: OAuthManager | None = None


def set_google_client(client: GoogleCalendarClient) -> None:
    global _gcal
    _gcal = client


def set_oauth_manager(oauth: OAuthManager) -> None:
    global _oauth
    _oauth = oauth


def _get_client() -> GoogleCalendarClient:
    if _gcal is None:
        raise RuntimeError("GoogleCalendarClient not initialized")
    return _gcal


async def _get_creds(agent_id: str):
    if _oauth is None:
        raise RuntimeError("OAuthManager not initialized")
    creds = await _oauth.get_credentials(agent_id)
    if creds is None:
        auth_url = _oauth.get_auth_url(agent_id)
        raise RuntimeError(
            f"Google account not connected. Please authorize here: {auth_url}"
        )
    return creds


def _format_task(task: dict) -> dict:
    """Extract key fields from a Google Tasks task into a clean dict."""
    return {
        "task_id": task.get("id", ""),
        "title": task.get("title", "(No title)"),
        "notes": task.get("notes"),
        "status": task.get("status", "needsAction"),
        "due": task.get("due"),
        "completed": task.get("completed"),
        "parent": task.get("parent"),
        "position": task.get("position"),
        "updated": task.get("updated"),
        "is_completed": task.get("status") == "completed",
        "links": task.get("links", []),
    }


# ══════════════════════════════════════════════════════════════
#  TASK LIST MANAGEMENT (2)
# ══════════════════════════════════════════════════════════════


@task_mcp.tool()
async def list_task_lists(agent_id: str) -> str:
    """List all of the user's task lists.

    Shows lists like "My Tasks", "Work", or any custom lists they've created.

    Args:
        agent_id: The agent/user ID
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        lists = await client.list_task_lists(creds)

        task_lists = []
        for tl in lists:
            task_lists.append({
                "list_id": tl.get("id"),
                "title": tl.get("title", "(Untitled)"),
                "updated": tl.get("updated"),
            })

        return json.dumps({
            "status": "ok",
            "count": len(task_lists),
            "task_lists": task_lists,
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] list_task_lists failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def create_task_list(
    agent_id: str,
    title: str,
) -> str:
    """Create a new task list.

    Example: "Shopping", "Project Alpha", "Fitness Goals"

    Args:
        agent_id: The agent/user ID
        title: Name for the new task list
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        tl = await client.create_task_list(creds, title)
        return json.dumps({
            "status": "success",
            "message": f"Task list '{title}' created.",
            "task_list": {
                "list_id": tl.get("id"),
                "title": tl.get("title"),
            },
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] create_task_list failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  TASK CRUD (6)
# ══════════════════════════════════════════════════════════════


@task_mcp.tool()
async def list_tasks(
    agent_id: str,
    list_id: str = "@default",
    show_completed: str = "false",
    due_before: str = "",
    due_after: str = "",
    max_results: str = "50",
) -> str:
    """List tasks in a task list.

    By default returns only pending (non-completed) tasks.

    Args:
        agent_id: The agent/user ID
        list_id: Task list ID (default: "@default" = "My Tasks"). Use list_task_lists to find IDs.
        show_completed: "true" to include completed tasks
        due_before: Only tasks due before this date (RFC 3339, e.g. "2026-04-10T00:00:00Z")
        due_after: Only tasks due after this date (RFC 3339)
        max_results: Maximum tasks to return (default: "50")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        try:
            max_r = int(max_results)
        except ValueError:
            max_r = 50

        tasks = await client.list_tasks(
            creds,
            tasklist_id=list_id,
            show_completed=show_completed.lower() == "true",
            due_min=due_after or None,
            due_max=due_before or None,
            max_results=max_r,
        )

        # Separate into pending and completed
        pending = [t for t in tasks if t.get("status") != "completed"]
        completed = [t for t in tasks if t.get("status") == "completed"]

        return json.dumps({
            "status": "ok",
            "list_id": list_id,
            "pending_count": len(pending),
            "completed_count": len(completed),
            "total": len(tasks),
            "tasks": [_format_task(t) for t in tasks],
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] list_tasks failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def create_task(
    agent_id: str,
    title: str,
    list_id: str = "@default",
    notes: str = "",
    due_date: str = "",
    parent_task_id: str = "",
) -> str:
    """Create a new task.

    Supports subtasks by specifying a parent_task_id.

    Args:
        agent_id: The agent/user ID
        title: Task title (e.g., "Buy groceries", "Review PR #42")
        list_id: Task list ID (default: "@default" = "My Tasks")
        notes: Additional notes or description
        due_date: Due date (YYYY-MM-DD or RFC 3339). Example: "2026-04-10"
        parent_task_id: Parent task ID to create this as a subtask
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        # Convert date-only to RFC 3339
        due_rfc = None
        if due_date:
            if "T" not in due_date:
                due_rfc = f"{due_date}T00:00:00.000Z"
            else:
                due_rfc = due_date

        task = await client.create_task(
            creds,
            title=title,
            tasklist_id=list_id,
            notes=notes or None,
            due=due_rfc,
            parent=parent_task_id or None,
        )

        return json.dumps({
            "status": "success",
            "message": f"Task '{title}' created." + (f" Due: {due_date}" if due_date else ""),
            "task": _format_task(task),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] create_task failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def update_task(
    agent_id: str,
    task_id: str,
    list_id: str = "@default",
    title: str = "",
    notes: str = "",
    due_date: str = "",
) -> str:
    """Update an existing task. Only provide fields you want to change.

    Args:
        agent_id: The agent/user ID
        task_id: The task ID to update
        list_id: Task list containing the task
        title: New title
        notes: New notes
        due_date: New due date (YYYY-MM-DD or RFC 3339)
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        updates: dict = {}
        if title:
            updates["title"] = title
        if notes:
            updates["notes"] = notes
        if due_date:
            if "T" not in due_date:
                updates["due"] = f"{due_date}T00:00:00.000Z"
            else:
                updates["due"] = due_date

        if not updates:
            return json.dumps({"status": "error", "message": "No fields provided to update."})

        task = await client.update_task(creds, list_id, task_id, updates)
        return json.dumps({
            "status": "success",
            "message": "Task updated.",
            "task": _format_task(task),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] update_task failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def complete_task(
    agent_id: str,
    task_id: str,
    list_id: str = "@default",
) -> str:
    """Mark a task as completed.

    Args:
        agent_id: The agent/user ID
        task_id: The task ID to complete
        list_id: Task list containing the task
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        task = await client.complete_task(creds, list_id, task_id)
        title = task.get("title", "")
        return json.dumps({
            "status": "success",
            "message": f"Task '{title}' marked as complete. ✓",
            "task": _format_task(task),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] complete_task failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def delete_task(
    agent_id: str,
    task_id: str,
    list_id: str = "@default",
) -> str:
    """Delete a task permanently.

    Args:
        agent_id: The agent/user ID
        task_id: The task ID to delete
        list_id: Task list containing the task
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        await client.delete_task(creds, list_id, task_id)
        return json.dumps({
            "status": "success",
            "message": f"Task {task_id} deleted.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] delete_task failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@task_mcp.tool()
async def clear_completed_tasks(
    agent_id: str,
    list_id: str = "@default",
) -> str:
    """Remove all completed tasks from a task list. Cleans up finished items.

    Args:
        agent_id: The agent/user ID
        list_id: Task list to clear completed tasks from (default: "My Tasks")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        await client.clear_completed_tasks(creds, list_id)
        return json.dumps({
            "status": "success",
            "message": "All completed tasks cleared from the list.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] clear_completed_tasks failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})
