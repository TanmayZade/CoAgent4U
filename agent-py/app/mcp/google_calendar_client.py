"""Async Google Calendar API v3 + Tasks API v1 client.

Thin wrapper using httpx + google-auth for direct REST API access.
No dependency on google-api-python-client — stays lightweight and async.

All methods return raw dicts from the Google API responses.
The MCP tool layers parse these into user-friendly formats.
"""
import logging
import uuid
from datetime import datetime, timezone as dt_timezone
from typing import Any

import httpx
from google.oauth2.credentials import Credentials
from google.auth.transport.requests import Request as GoogleAuthRequest

logger = logging.getLogger(__name__)

# ── API Base URLs ─────────────────────────────────────────────
CALENDAR_API = "https://www.googleapis.com/calendar/v3"
TASKS_API = "https://tasks.googleapis.com/tasks/v1"


class GoogleCalendarClient:
    """Async client for Google Calendar API v3 and Tasks API v1.

    Each method accepts a `creds` parameter (google.oauth2.credentials.Credentials)
    so the caller (MCP tools) can pass per-agent credentials.
    """

    def __init__(self):
        self._http = httpx.AsyncClient(timeout=30.0)
        logger.info("[GCalClient] Initialized")

    async def close(self):
        await self._http.aclose()

    # ── Internal: Authenticated request ───────────────────────

    def _auth_headers(self, creds: Credentials) -> dict:
        """Build auth headers, refreshing token if needed."""
        if creds.expired and creds.refresh_token:
            creds.refresh(GoogleAuthRequest())
        return {
            "Authorization": f"Bearer {creds.token}",
            "Content-Type": "application/json",
        }

    async def _get(self, url: str, creds: Credentials, params: dict | None = None) -> Any:
        headers = self._auth_headers(creds)
        resp = await self._http.get(url, headers=headers, params=params)
        resp.raise_for_status()
        return resp.json()

    async def _post(self, url: str, creds: Credentials, data: dict | None = None, params: dict | None = None) -> Any:
        headers = self._auth_headers(creds)
        resp = await self._http.post(url, headers=headers, json=data, params=params)
        resp.raise_for_status()
        return resp.json()

    async def _patch(self, url: str, creds: Credentials, data: dict | None = None) -> Any:
        headers = self._auth_headers(creds)
        resp = await self._http.patch(url, headers=headers, json=data)
        resp.raise_for_status()
        return resp.json()

    async def _put(self, url: str, creds: Credentials, data: dict | None = None) -> Any:
        headers = self._auth_headers(creds)
        resp = await self._http.put(url, headers=headers, json=data)
        resp.raise_for_status()
        return resp.json()

    async def _delete(self, url: str, creds: Credentials, params: dict | None = None) -> dict:
        headers = self._auth_headers(creds)
        resp = await self._http.delete(url, headers=headers, params=params)
        resp.raise_for_status()
        # DELETE often returns 204 No Content
        if resp.status_code == 204:
            return {"status": "deleted"}
        try:
            return resp.json()
        except Exception:
            return {"status": "deleted"}

    # ══════════════════════════════════════════════════════════
    #  CALENDAR API v3 — Events
    # ══════════════════════════════════════════════════════════

    async def list_events(
        self,
        creds: Credentials,
        calendar_id: str = "primary",
        time_min: str | None = None,
        time_max: str | None = None,
        query: str | None = None,
        max_results: int = 50,
        single_events: bool = True,
        order_by: str = "startTime",
    ) -> list[dict]:
        """List events from a calendar.

        Args:
            calendar_id: Calendar ID (default: "primary")
            time_min: Lower bound (ISO 8601, e.g. "2026-04-07T00:00:00Z")
            time_max: Upper bound (ISO 8601)
            query: Free-text search query
            max_results: Max events to return
            single_events: If True, expand recurring events into instances
            order_by: "startTime" or "updated"
        """
        params: dict[str, Any] = {
            "maxResults": max_results,
            "singleEvents": str(single_events).lower(),
        }
        if order_by and single_events:
            params["orderBy"] = order_by
        if time_min:
            params["timeMin"] = time_min
        if time_max:
            params["timeMax"] = time_max
        if query:
            params["q"] = query

        url = f"{CALENDAR_API}/calendars/{calendar_id}/events"
        result = await self._get(url, creds, params=params)
        return result.get("items", [])

    async def get_event(
        self, creds: Credentials, event_id: str, calendar_id: str = "primary"
    ) -> dict:
        """Get a single event by ID."""
        url = f"{CALENDAR_API}/calendars/{calendar_id}/events/{event_id}"
        return await self._get(url, creds)

    async def create_event(
        self,
        creds: Credentials,
        summary: str,
        start: str,
        end: str,
        calendar_id: str = "primary",
        description: str | None = None,
        location: str | None = None,
        recurrence: list[str] | None = None,
        reminders: dict | None = None,
        color_id: str | None = None,
        conference: bool = False,
        timezone: str | None = None,
    ) -> dict:
        """Create a new event.

        Args:
            summary: Event title
            start: Start time (ISO 8601) or date (YYYY-MM-DD for all-day)
            end: End time (ISO 8601) or date (YYYY-MM-DD for all-day)
            calendar_id: Calendar to create in
            description: Event description/notes
            location: Event location
            recurrence: RRULE strings, e.g. ["RRULE:FREQ=WEEKLY;COUNT=10"]
            reminders: Override reminders, e.g. {"useDefault": False, "overrides": [{"method": "popup", "minutes": 10}]}
            color_id: Event color ID (1-11)
            conference: If True, auto-create a Google Meet link
            timezone: Timezone for start/end (e.g. "Asia/Kolkata")
        """
        # Determine if all-day event (date only, no 'T')
        is_all_day = "T" not in start

        if is_all_day:
            start_body = {"date": start}
            end_body = {"date": end}
        else:
            start_body = {"dateTime": start}
            end_body = {"dateTime": end}

        if timezone:
            start_body["timeZone"] = timezone
            end_body["timeZone"] = timezone

        body: dict[str, Any] = {
            "summary": summary,
            "start": start_body,
            "end": end_body,
        }

        if description:
            body["description"] = description
        if location:
            body["location"] = location
        if recurrence:
            body["recurrence"] = recurrence
        if reminders:
            body["reminders"] = reminders
        if color_id:
            body["colorId"] = color_id
        if conference:
            body["conferenceData"] = {
                "createRequest": {
                    "requestId": f"coagent-{uuid.uuid4().hex[:12]}",
                    "conferenceSolutionKey": {"type": "hangoutsMeet"},
                }
            }

        url = f"{CALENDAR_API}/calendars/{calendar_id}/events"
        params = {}
        if conference:
            params["conferenceDataVersion"] = "1"

        return await self._post(url, creds, data=body, params=params if params else None)

    async def update_event(
        self,
        creds: Credentials,
        event_id: str,
        updates: dict,
        calendar_id: str = "primary",
    ) -> dict:
        """Update an existing event (PATCH — partial update).

        Args:
            event_id: The event ID to update
            updates: Dict of fields to update (e.g. {"summary": "New Title", "location": "Office"})
            calendar_id: Calendar containing the event
        """
        url = f"{CALENDAR_API}/calendars/{calendar_id}/events/{event_id}"
        return await self._patch(url, creds, data=updates)

    async def delete_event(
        self, creds: Credentials, event_id: str, calendar_id: str = "primary"
    ) -> dict:
        """Delete an event."""
        url = f"{CALENDAR_API}/calendars/{calendar_id}/events/{event_id}"
        return await self._delete(url, creds)

    async def quick_add(
        self, creds: Credentials, text: str, calendar_id: str = "primary"
    ) -> dict:
        """Create an event from natural language text.

        Google parses the text and creates the event automatically.
        Example: "Dinner at 7pm tomorrow" or "Meeting with team on Friday 3-4pm"
        """
        url = f"{CALENDAR_API}/calendars/{calendar_id}/events/quickAdd"
        return await self._post(url, creds, params={"text": text})

    async def move_event(
        self,
        creds: Credentials,
        event_id: str,
        source_calendar_id: str,
        destination_calendar_id: str,
    ) -> dict:
        """Move an event from one calendar to another."""
        url = f"{CALENDAR_API}/calendars/{source_calendar_id}/events/{event_id}/move"
        return await self._post(url, creds, params={"destination": destination_calendar_id})

    async def get_event_instances(
        self,
        creds: Credentials,
        event_id: str,
        calendar_id: str = "primary",
        time_min: str | None = None,
        time_max: str | None = None,
        max_results: int = 25,
    ) -> list[dict]:
        """Get instances of a recurring event."""
        params: dict[str, Any] = {"maxResults": max_results}
        if time_min:
            params["timeMin"] = time_min
        if time_max:
            params["timeMax"] = time_max

        url = f"{CALENDAR_API}/calendars/{calendar_id}/events/{event_id}/instances"
        result = await self._get(url, creds, params=params)
        return result.get("items", [])

    # ══════════════════════════════════════════════════════════
    #  CALENDAR API v3 — Freebusy
    # ══════════════════════════════════════════════════════════

    async def query_free_busy(
        self,
        creds: Credentials,
        time_min: str,
        time_max: str,
        calendar_ids: list[str] | None = None,
    ) -> dict:
        """Query free/busy information for the user's calendars.

        Args:
            time_min: Start of time range (ISO 8601)
            time_max: End of time range (ISO 8601)
            calendar_ids: Specific calendars to check (default: ["primary"])
        """
        if not calendar_ids:
            calendar_ids = ["primary"]

        body = {
            "timeMin": time_min,
            "timeMax": time_max,
            "items": [{"id": cid} for cid in calendar_ids],
        }
        url = f"{CALENDAR_API}/freeBusy"
        return await self._post(url, creds, data=body)

    # ══════════════════════════════════════════════════════════
    #  CALENDAR API v3 — CalendarList
    # ══════════════════════════════════════════════════════════

    async def list_calendars(self, creds: Credentials) -> list[dict]:
        """List all calendars on the user's calendar list."""
        url = f"{CALENDAR_API}/users/me/calendarList"
        result = await self._get(url, creds)
        return result.get("items", [])

    async def get_calendar(self, creds: Credentials, calendar_id: str) -> dict:
        """Get metadata for a specific calendar."""
        url = f"{CALENDAR_API}/users/me/calendarList/{calendar_id}"
        return await self._get(url, creds)

    async def create_calendar(self, creds: Credentials, summary: str, description: str | None = None, timezone: str | None = None) -> dict:
        """Create a new secondary calendar."""
        body: dict[str, Any] = {"summary": summary}
        if description:
            body["description"] = description
        if timezone:
            body["timeZone"] = timezone
        url = f"{CALENDAR_API}/calendars"
        return await self._post(url, creds, data=body)

    async def delete_calendar(self, creds: Credentials, calendar_id: str) -> dict:
        """Delete a secondary calendar."""
        url = f"{CALENDAR_API}/calendars/{calendar_id}"
        return await self._delete(url, creds)

    async def clear_calendar(self, creds: Credentials, calendar_id: str = "primary") -> dict:
        """Clear all events from a calendar."""
        url = f"{CALENDAR_API}/calendars/{calendar_id}/clear"
        headers = self._auth_headers(creds)
        resp = await self._http.post(url, headers=headers)
        resp.raise_for_status()
        return {"status": "cleared", "calendar_id": calendar_id}

    # ══════════════════════════════════════════════════════════
    #  CALENDAR API v3 — Colors & Settings
    # ══════════════════════════════════════════════════════════

    async def list_colors(self, creds: Credentials) -> dict:
        """Get available calendar and event colors."""
        url = f"{CALENDAR_API}/colors"
        return await self._get(url, creds)

    async def list_settings(self, creds: Credentials) -> list[dict]:
        """Get user's calendar settings (timezone, date format, etc.)."""
        url = f"{CALENDAR_API}/users/me/settings"
        result = await self._get(url, creds)
        return result.get("items", [])

    # ══════════════════════════════════════════════════════════
    #  TASKS API v1 — Task Lists
    # ══════════════════════════════════════════════════════════

    async def list_task_lists(self, creds: Credentials) -> list[dict]:
        """List all task lists."""
        url = f"{TASKS_API}/users/@me/lists"
        result = await self._get(url, creds)
        return result.get("items", [])

    async def create_task_list(self, creds: Credentials, title: str) -> dict:
        """Create a new task list."""
        url = f"{TASKS_API}/users/@me/lists"
        return await self._post(url, creds, data={"title": title})

    async def delete_task_list(self, creds: Credentials, tasklist_id: str) -> dict:
        """Delete a task list."""
        url = f"{TASKS_API}/users/@me/lists/{tasklist_id}"
        return await self._delete(url, creds)

    # ══════════════════════════════════════════════════════════
    #  TASKS API v1 — Tasks
    # ══════════════════════════════════════════════════════════

    async def list_tasks(
        self,
        creds: Credentials,
        tasklist_id: str = "@default",
        show_completed: bool = False,
        show_hidden: bool = False,
        due_min: str | None = None,
        due_max: str | None = None,
        max_results: int = 100,
    ) -> list[dict]:
        """List tasks in a task list.

        Args:
            tasklist_id: Task list ID (default: "@default" for "My Tasks")
            show_completed: Include completed tasks
            show_hidden: Include hidden/deleted tasks
            due_min: Lower bound for due date (RFC 3339)
            due_max: Upper bound for due date (RFC 3339)
        """
        params: dict[str, Any] = {
            "maxResults": max_results,
            "showCompleted": str(show_completed).lower(),
            "showHidden": str(show_hidden).lower(),
        }
        if due_min:
            params["dueMin"] = due_min
        if due_max:
            params["dueMax"] = due_max

        url = f"{TASKS_API}/lists/{tasklist_id}/tasks"
        result = await self._get(url, creds, params=params)
        return result.get("items", [])

    async def get_task(self, creds: Credentials, tasklist_id: str, task_id: str) -> dict:
        """Get a specific task."""
        url = f"{TASKS_API}/lists/{tasklist_id}/tasks/{task_id}"
        return await self._get(url, creds)

    async def create_task(
        self,
        creds: Credentials,
        title: str,
        tasklist_id: str = "@default",
        notes: str | None = None,
        due: str | None = None,
        parent: str | None = None,
    ) -> dict:
        """Create a new task.

        Args:
            title: Task title
            tasklist_id: Which task list to create in
            notes: Additional notes/description
            due: Due date in RFC 3339 format (e.g. "2026-04-10T00:00:00.000Z")
            parent: Parent task ID (to create a subtask)
        """
        body: dict[str, Any] = {"title": title}
        if notes:
            body["notes"] = notes
        if due:
            body["due"] = due

        url = f"{TASKS_API}/lists/{tasklist_id}/tasks"
        params = {}
        if parent:
            params["parent"] = parent

        return await self._post(url, creds, data=body, params=params if params else None)

    async def update_task(
        self,
        creds: Credentials,
        tasklist_id: str,
        task_id: str,
        updates: dict,
    ) -> dict:
        """Update a task (PATCH).

        Args:
            updates: Fields to update — title, notes, due, status ("needsAction" or "completed")
        """
        url = f"{TASKS_API}/lists/{tasklist_id}/tasks/{task_id}"
        return await self._patch(url, creds, data=updates)

    async def complete_task(
        self, creds: Credentials, tasklist_id: str, task_id: str
    ) -> dict:
        """Mark a task as completed."""
        return await self.update_task(
            creds, tasklist_id, task_id,
            updates={
                "status": "completed",
                "completed": datetime.now(tz=dt_timezone.utc).isoformat(),
            },
        )

    async def uncomplete_task(
        self, creds: Credentials, tasklist_id: str, task_id: str
    ) -> dict:
        """Mark a completed task back as needs action."""
        return await self.update_task(
            creds, tasklist_id, task_id,
            updates={"status": "needsAction", "completed": None},
        )

    async def delete_task(
        self, creds: Credentials, tasklist_id: str, task_id: str
    ) -> dict:
        """Delete a task."""
        url = f"{TASKS_API}/lists/{tasklist_id}/tasks/{task_id}"
        return await self._delete(url, creds)

    async def clear_completed_tasks(self, creds: Credentials, tasklist_id: str) -> dict:
        """Clear all completed tasks from a task list."""
        url = f"{TASKS_API}/lists/{tasklist_id}/clear"
        headers = self._auth_headers(creds)
        resp = await self._http.post(url, headers=headers)
        resp.raise_for_status()
        return {"status": "cleared", "tasklist_id": tasklist_id}

    async def move_task(
        self,
        creds: Credentials,
        tasklist_id: str,
        task_id: str,
        parent: str | None = None,
        previous: str | None = None,
    ) -> dict:
        """Move/reorder a task within its list.

        Args:
            parent: New parent task ID (None = move to top level)
            previous: Previous sibling task ID (for ordering)
        """
        url = f"{TASKS_API}/lists/{tasklist_id}/tasks/{task_id}/move"
        params: dict[str, str] = {}
        if parent:
            params["parent"] = parent
        if previous:
            params["previous"] = previous
        return await self._post(url, creds, params=params if params else None)
