"""MCP Calendar Tools — Direct Google Calendar API v3 integration.

17 personal calendar tools exposed via the Model Context Protocol.
No Java bridge — talks directly to Google Calendar API using OAuth 2.0.

Tools:
  Event Management (8):  get_upcoming_events, search_events, get_event_details,
                         create_event, update_event, delete_event, quick_add_event, move_event
  Availability (2):      find_free_slots, get_recurring_event_instances
  Calendar Mgmt (4):     list_calendars, create_calendar, delete_calendar, clear_calendar
  Auth & Settings (3):   get_auth_status, get_auth_url, get_calendar_settings
"""
import json
import logging
from datetime import date, datetime, timedelta, timezone

from fastmcp import FastMCP

from app.mcp.google_calendar_client import GoogleCalendarClient
from app.mcp.oauth_manager import OAuthManager

logger = logging.getLogger(__name__)

# ── MCP Server Instance ──────────────────────────────────────

calendar_mcp = FastMCP(
    "CoAgent Calendar Server",
    client_log_level="DEBUG",
    instructions=(
        "You are a personal calendar assistant. "
        "Use these tools to view, search, and manage the user's Google Calendar. "
        "Always confirm with the user before creating or deleting events."
    ),
)

# ── Injected at startup from main.py ─────────────────────────
_gcal: GoogleCalendarClient | None = None
_oauth: OAuthManager | None = None


def set_google_client(client: GoogleCalendarClient) -> None:
    global _gcal
    _gcal = client
    logger.info("[MCP] Google Calendar client connected")


def set_oauth_manager(oauth: OAuthManager) -> None:
    global _oauth
    _oauth = oauth
    logger.info("[MCP] OAuth manager connected")


def _get_client() -> GoogleCalendarClient:
    if _gcal is None:
        raise RuntimeError("GoogleCalendarClient not initialized")
    return _gcal


def _get_oauth() -> OAuthManager:
    if _oauth is None:
        raise RuntimeError("OAuthManager not initialized")
    return _oauth


async def _get_creds(agent_id: str):
    """Get Google credentials for an agent, raising a helpful error if not authorized."""
    oauth = _get_oauth()
    creds = await oauth.get_credentials(agent_id)
    if creds is None:
        auth_url = oauth.get_auth_url(agent_id)
        raise RuntimeError(
            f"Google Calendar not connected. Please authorize here: {auth_url}"
        )
    return creds


def _format_event(event: dict) -> dict:
    """Extract key fields from a Google Calendar event into a clean dict."""
    start = event.get("start", {})
    end = event.get("end", {})
    return {
        "event_id": event.get("id", ""),
        "title": event.get("summary", "(No title)"),
        "start": start.get("dateTime") or start.get("date", ""),
        "end": end.get("dateTime") or end.get("date", ""),
        "location": event.get("location"),
        "description": event.get("description"),
        "status": event.get("status"),
        "html_link": event.get("htmlLink"),
        "is_recurring": bool(event.get("recurringEventId")),
        "conference_link": _extract_meet_link(event),
        "calendar_id": event.get("organizer", {}).get("email"),
    }


def _extract_meet_link(event: dict) -> str | None:
    """Extract Google Meet link from event conference data."""
    conf = event.get("conferenceData")
    if conf:
        for ep in conf.get("entryPoints", []):
            if ep.get("entryPointType") == "video":
                return ep.get("uri")
    return None


def _ensure_rfc3339(dt_str: str) -> str:
    """Ensure a date string is RFC 3339 / ISO 8601 with timezone.

    Handles common formats:
      - "2026-04-07" → "2026-04-07T00:00:00Z"
      - "2026-04-07T10:00:00" → "2026-04-07T10:00:00Z"
      - "2026-04-07T10:00:00+05:30" → unchanged
      - "2026-04-07T10:00:00Z" → unchanged
    """
    if not dt_str:
        return datetime.now(tz=timezone.utc).isoformat()

    # Date-only
    if "T" not in dt_str:
        return f"{dt_str}T00:00:00Z"

    # Already has timezone
    if dt_str.endswith("Z") or "+" in dt_str[10:] or dt_str.count("-") > 2:
        return dt_str

    # No timezone — assume UTC
    return f"{dt_str}Z"


# ══════════════════════════════════════════════════════════════
#  EVENT MANAGEMENT TOOLS (8)
# ══════════════════════════════════════════════════════════════


@calendar_mcp.tool()
async def get_upcoming_events(
    agent_id: str,
    start_date: str = "",
    end_date: str = "",
    days_ahead: str = "7",
    calendar_id: str = "primary",
    max_results: str = "25",
) -> str:
    """Get the user's upcoming calendar events.

    Use when the user asks about their schedule, upcoming events,
    or what they have planned.

    Args:
        agent_id: The agent/user ID
        start_date: Start date (YYYY-MM-DD). Defaults to today.
        end_date: End date (YYYY-MM-DD). Defaults to start_date + days_ahead.
        days_ahead: Number of days to look ahead (default: "7").
        calendar_id: Calendar to query (default: "primary").
        max_results: Maximum events to return (default: "25").
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not start_date:
            start_date = date.today().isoformat()
        if not end_date:
            try:
                days = int(days_ahead)
            except ValueError:
                days = 7
            end_date = (date.fromisoformat(start_date) + timedelta(days=days)).isoformat()

        time_min = _ensure_rfc3339(start_date)
        time_max = _ensure_rfc3339(end_date)

        try:
            max_r = int(max_results)
        except ValueError:
            max_r = 25

        events = await client.list_events(
            creds, calendar_id=calendar_id,
            time_min=time_min, time_max=time_max,
            max_results=max_r,
        )

        return json.dumps({
            "status": "ok",
            "count": len(events),
            "date_range": {"start": start_date, "end": end_date},
            "events": [_format_event(e) for e in events],
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] get_upcoming_events failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def search_events(
    agent_id: str,
    query: str,
    start_date: str = "",
    end_date: str = "",
    days_ahead: str = "30",
    calendar_id: str = "primary",
) -> str:
    """Search for events by keyword across the user's calendar.

    Use when the user asks "do I have anything about X" or "find my dentist appointment".

    Args:
        agent_id: The agent/user ID
        query: Search text (matches event title, description, location, attendees)
        start_date: Search from this date (YYYY-MM-DD). Defaults to today.
        end_date: Search until this date. Defaults to start_date + days_ahead.
        days_ahead: Number of days to search (default: "30").
        calendar_id: Calendar to search (default: "primary" = all visible).
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not start_date:
            start_date = date.today().isoformat()
        if not end_date:
            try:
                days = int(days_ahead)
            except ValueError:
                days = 30
            end_date = (date.fromisoformat(start_date) + timedelta(days=days)).isoformat()

        events = await client.list_events(
            creds, calendar_id=calendar_id,
            time_min=_ensure_rfc3339(start_date),
            time_max=_ensure_rfc3339(end_date),
            query=query, max_results=50,
        )

        return json.dumps({
            "status": "ok",
            "query": query,
            "count": len(events),
            "events": [_format_event(e) for e in events],
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] search_events failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def get_event_details(
    agent_id: str,
    event_id: str,
    calendar_id: str = "primary",
) -> str:
    """Get full details of a specific event by its ID.

    Use when the user wants more info about a specific event.

    Args:
        agent_id: The agent/user ID
        event_id: The Google Calendar event ID
        calendar_id: Calendar containing the event (default: "primary")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        event = await client.get_event(creds, event_id, calendar_id)

        start = event.get("start", {})
        end = event.get("end", {})
        reminders = event.get("reminders", {})

        detail = {
            "event_id": event.get("id"),
            "title": event.get("summary", "(No title)"),
            "start": start.get("dateTime") or start.get("date", ""),
            "end": end.get("dateTime") or end.get("date", ""),
            "location": event.get("location"),
            "description": event.get("description"),
            "status": event.get("status"),
            "created": event.get("created"),
            "updated": event.get("updated"),
            "html_link": event.get("htmlLink"),
            "recurrence": event.get("recurrence"),
            "is_recurring": bool(event.get("recurringEventId")),
            "recurring_event_id": event.get("recurringEventId"),
            "color_id": event.get("colorId"),
            "conference_link": _extract_meet_link(event),
            "reminders": reminders,
            "creator": event.get("creator", {}).get("email"),
            "visibility": event.get("visibility", "default"),
        }

        return json.dumps({"status": "ok", "event": detail})

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] get_event_details failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def create_event(
    agent_id: str,
    title: str,
    start: str,
    end: str,
    calendar_id: str = "primary",
    description: str = "",
    location: str = "",
    recurrence: str = "",
    reminder_minutes: str = "",
    color_id: str = "",
    add_meet_link: str = "false",
    timezone: str = "",
) -> str:
    """Create a new calendar event.

    Args:
        agent_id: The agent/user ID
        title: Event title/name
        start: Start time (ISO 8601, e.g. "2026-04-10T10:00:00+05:30") or date for all-day ("2026-04-10")
        end: End time (ISO 8601) or next-day date for all-day events
        calendar_id: Calendar to create in (default: "primary")
        description: Event description/notes
        location: Event location
        recurrence: RRULE string, e.g. "RRULE:FREQ=WEEKLY;COUNT=10" or "RRULE:FREQ=DAILY;UNTIL=20260501"
        reminder_minutes: Comma-separated popup reminder minutes, e.g. "10,30" for 10 and 30 min before
        color_id: Event color (1-11). 1=Lavender, 2=Sage, 3=Grape, 4=Flamingo, 5=Banana, 6=Tangerine, 7=Peacock, 8=Graphite, 9=Blueberry, 10=Basil, 11=Tomato
        add_meet_link: "true" to auto-create a Google Meet link
        timezone: Timezone (e.g. "Asia/Kolkata", "America/New_York")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        # Parse recurrence
        recurrence_list = [recurrence] if recurrence else None

        # Parse reminders
        reminders = None
        if reminder_minutes:
            try:
                mins = [int(m.strip()) for m in reminder_minutes.split(",")]
                reminders = {
                    "useDefault": False,
                    "overrides": [{"method": "popup", "minutes": m} for m in mins],
                }
            except ValueError:
                pass

        event = await client.create_event(
            creds,
            summary=title,
            start=start,
            end=end,
            calendar_id=calendar_id,
            description=description or None,
            location=location or None,
            recurrence=recurrence_list,
            reminders=reminders,
            color_id=color_id or None,
            conference=add_meet_link.lower() == "true",
            timezone=timezone or None,
        )

        return json.dumps({
            "status": "success",
            "message": f"Event '{title}' created successfully.",
            "event": _format_event(event),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] create_event failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def update_event(
    agent_id: str,
    event_id: str,
    calendar_id: str = "primary",
    title: str = "",
    start: str = "",
    end: str = "",
    description: str = "",
    location: str = "",
    color_id: str = "",
) -> str:
    """Update an existing calendar event. Only provide the fields you want to change.

    Args:
        agent_id: The agent/user ID
        event_id: The event ID to update
        calendar_id: Calendar containing the event
        title: New title (leave empty to keep current)
        start: New start time (ISO 8601)
        end: New end time (ISO 8601)
        description: New description
        location: New location
        color_id: New color ID (1-11)
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        updates: dict = {}
        if title:
            updates["summary"] = title
        if description:
            updates["description"] = description
        if location:
            updates["location"] = location
        if color_id:
            updates["colorId"] = color_id
        if start:
            is_all_day = "T" not in start
            updates["start"] = {"date": start} if is_all_day else {"dateTime": start}
        if end:
            is_all_day = "T" not in end
            updates["end"] = {"date": end} if is_all_day else {"dateTime": end}

        if not updates:
            return json.dumps({"status": "error", "message": "No fields provided to update."})

        event = await client.update_event(creds, event_id, updates, calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"Event updated successfully.",
            "event": _format_event(event),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] update_event failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def delete_event(
    agent_id: str,
    event_id: str,
    calendar_id: str = "primary",
) -> str:
    """Delete a calendar event.

    Args:
        agent_id: The agent/user ID
        event_id: The event ID to delete
        calendar_id: Calendar containing the event
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        await client.delete_event(creds, event_id, calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"Event {event_id} deleted.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] delete_event failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def quick_add_event(
    agent_id: str,
    text: str,
    calendar_id: str = "primary",
) -> str:
    """Create an event from natural language text. Google interprets the text automatically.

    Examples: "Dentist appointment Friday at 3pm", "Gym session every Monday at 6am",
    "Call mom tomorrow 5:30pm for 30 minutes"

    Args:
        agent_id: The agent/user ID
        text: Natural language description of the event
        calendar_id: Calendar to create in (default: "primary")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        event = await client.quick_add(creds, text, calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"Event created from text: '{text}'",
            "event": _format_event(event),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] quick_add_event failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def move_event(
    agent_id: str,
    event_id: str,
    source_calendar_id: str,
    destination_calendar_id: str,
) -> str:
    """Move an event from one of the user's calendars to another.

    Args:
        agent_id: The agent/user ID
        event_id: The event ID to move
        source_calendar_id: Current calendar ID
        destination_calendar_id: Target calendar ID
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        event = await client.move_event(creds, event_id, source_calendar_id, destination_calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"Event moved to calendar '{destination_calendar_id}'.",
            "event": _format_event(event),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] move_event failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  AVAILABILITY TOOLS (2)
# ══════════════════════════════════════════════════════════════


@calendar_mcp.tool()
async def find_free_slots(
    agent_id: str,
    start_date: str = "",
    end_date: str = "",
    days_ahead: str = "3",
) -> str:
    """Find when the user is free or busy across their calendars.

    Returns busy time slots — any time NOT listed is free.

    Args:
        agent_id: The agent/user ID
        start_date: Start date (YYYY-MM-DD). Defaults to today.
        end_date: End date (YYYY-MM-DD). Defaults to start_date + days_ahead.
        days_ahead: Number of days to check (default: "3").
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not start_date:
            start_date = date.today().isoformat()
        if not end_date:
            try:
                days = int(days_ahead)
            except ValueError:
                days = 3
            end_date = (date.fromisoformat(start_date) + timedelta(days=days)).isoformat()

        result = await client.query_free_busy(
            creds,
            time_min=_ensure_rfc3339(start_date),
            time_max=_ensure_rfc3339(end_date),
        )

        # Extract busy slots from all calendars
        busy_slots = []
        calendars = result.get("calendars", {})
        for cal_id, cal_data in calendars.items():
            for busy in cal_data.get("busy", []):
                busy_slots.append({
                    "calendar": cal_id,
                    "start": busy.get("start"),
                    "end": busy.get("end"),
                })

        if not busy_slots:
            return json.dumps({
                "status": "ok",
                "message": f"You are completely free between {start_date} and {end_date}.",
                "busy_slots": [],
            })

        return json.dumps({
            "status": "ok",
            "busy_slot_count": len(busy_slots),
            "busy_slots": busy_slots,
            "note": "Any time NOT listed is free.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] find_free_slots failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def get_recurring_event_instances(
    agent_id: str,
    event_id: str,
    calendar_id: str = "primary",
    start_date: str = "",
    end_date: str = "",
    max_results: str = "10",
) -> str:
    """Get individual instances of a recurring event.

    Args:
        agent_id: The agent/user ID
        event_id: The recurring event ID
        calendar_id: Calendar containing the event
        start_date: Show instances from this date
        end_date: Show instances until this date
        max_results: Maximum instances to return
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        try:
            max_r = int(max_results)
        except ValueError:
            max_r = 10

        instances = await client.get_event_instances(
            creds, event_id, calendar_id,
            time_min=_ensure_rfc3339(start_date) if start_date else None,
            time_max=_ensure_rfc3339(end_date) if end_date else None,
            max_results=max_r,
        )

        return json.dumps({
            "status": "ok",
            "recurring_event_id": event_id,
            "instance_count": len(instances),
            "instances": [_format_event(e) for e in instances],
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] get_recurring_event_instances failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  CALENDAR MANAGEMENT TOOLS (4)
# ══════════════════════════════════════════════════════════════


@calendar_mcp.tool()
async def list_calendars(agent_id: str) -> str:
    """List all of the user's calendars.

    Shows calendar names, colors, and access levels.

    Args:
        agent_id: The agent/user ID
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        calendars = await client.list_calendars(creds)

        cal_list = []
        for cal in calendars:
            cal_list.append({
                "calendar_id": cal.get("id"),
                "name": cal.get("summary", "(No name)"),
                "description": cal.get("description"),
                "color": cal.get("backgroundColor"),
                "access_role": cal.get("accessRole"),
                "primary": cal.get("primary", False),
                "timezone": cal.get("timeZone"),
            })

        return json.dumps({
            "status": "ok",
            "count": len(cal_list),
            "calendars": cal_list,
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] list_calendars failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def create_calendar(
    agent_id: str,
    name: str,
    description: str = "",
    timezone: str = "",
) -> str:
    """Create a new secondary calendar.

    Use for organizing events by category (e.g., "Fitness", "Side Project", "Personal").

    Args:
        agent_id: The agent/user ID
        name: Calendar name
        description: Calendar description
        timezone: Calendar timezone (e.g., "Asia/Kolkata")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        cal = await client.create_calendar(
            creds, summary=name,
            description=description or None,
            timezone=timezone or None,
        )

        return json.dumps({
            "status": "success",
            "message": f"Calendar '{name}' created.",
            "calendar": {
                "calendar_id": cal.get("id"),
                "name": cal.get("summary"),
            },
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] create_calendar failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def delete_calendar(
    agent_id: str,
    calendar_id: str,
) -> str:
    """Delete a secondary calendar. Cannot delete the primary calendar.

    Args:
        agent_id: The agent/user ID
        calendar_id: The calendar ID to delete (NOT "primary")
    """
    try:
        if calendar_id == "primary":
            return json.dumps({
                "status": "error",
                "message": "Cannot delete the primary calendar. Use clear_calendar to remove all events instead.",
            })

        creds = await _get_creds(agent_id)
        client = _get_client()

        await client.delete_calendar(creds, calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"Calendar '{calendar_id}' deleted.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] delete_calendar failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@calendar_mcp.tool()
async def clear_calendar(
    agent_id: str,
    calendar_id: str = "primary",
) -> str:
    """Remove ALL events from a calendar. Use with caution!

    Args:
        agent_id: The agent/user ID
        calendar_id: Calendar to clear (default: "primary")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        await client.clear_calendar(creds, calendar_id)
        return json.dumps({
            "status": "success",
            "message": f"All events cleared from calendar '{calendar_id}'. This cannot be undone.",
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] clear_calendar failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  AUTH & SETTINGS TOOLS (3)
# ══════════════════════════════════════════════════════════════


@calendar_mcp.tool()
async def get_auth_status(agent_id: str) -> str:
    """Check if Google Calendar is connected for this user.

    Args:
        agent_id: The agent/user ID
    """
    oauth = _get_oauth()
    is_auth = oauth.is_authorized(agent_id)

    if is_auth:
        return json.dumps({
            "status": "ok",
            "connected": True,
            "message": "Google Calendar is connected and ready.",
        })
    else:
        auth_url = oauth.get_auth_url(agent_id)
        return json.dumps({
            "status": "ok",
            "connected": False,
            "message": "Google Calendar is not connected yet.",
            "auth_url": auth_url,
        })


@calendar_mcp.tool()
async def get_auth_url(agent_id: str) -> str:
    """Get the Google OAuth authorization URL so the user can connect their calendar.

    Args:
        agent_id: The agent/user ID
    """
    oauth = _get_oauth()
    auth_url = oauth.get_auth_url(agent_id)

    return json.dumps({
        "status": "ok",
        "auth_url": auth_url,
        "message": "Open this URL in a browser to connect your Google Calendar.",
    })


@calendar_mcp.tool()
async def get_calendar_settings(agent_id: str) -> str:
    """Get the user's Google Calendar settings (timezone, date format, etc.).

    Args:
        agent_id: The agent/user ID
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        settings = await client.list_settings(creds)

        settings_dict = {}
        for s in settings:
            settings_dict[s.get("id", "")] = s.get("value", "")

        return json.dumps({
            "status": "ok",
            "settings": settings_dict,
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] get_calendar_settings failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})
