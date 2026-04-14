"""MCP Productivity Tools — Smart features built on Calendar + Tasks.

7 personal productivity tools that compose calendar events and tasks data
to deliver actionable insights and automated time management.

Tools:
  Insights (2):     daily_briefing, weekly_summary
  Time Mgmt (2):    time_block_focus, schedule_with_preferences
  Daily Life (3):   set_event_reminders, create_recurring_routine, get_agenda
"""
import json
import logging
from datetime import date, datetime, timedelta, timezone

from fastmcp import FastMCP

from app.mcp.google_calendar_client import GoogleCalendarClient
from app.mcp.oauth_manager import OAuthManager

logger = logging.getLogger(__name__)

# ── MCP Server Instance ──────────────────────────────────────

productivity_mcp = FastMCP(
    "CoAgent Productivity Server",
    client_log_level="DEBUG",
    instructions=(
        "You are a personal productivity assistant. "
        "Use these tools to help the user understand their schedule, "
        "block focus time, set up routines, and stay on top of tasks."
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


def _ensure_rfc3339(dt_str: str) -> str:
    if not dt_str:
        return datetime.now(tz=timezone.utc).isoformat()
    if "T" not in dt_str:
        return f"{dt_str}T00:00:00Z"
    if dt_str.endswith("Z") or "+" in dt_str[10:] or dt_str.count("-") > 2:
        return dt_str
    return f"{dt_str}Z"


# ══════════════════════════════════════════════════════════════
#  INSIGHTS (2)
# ══════════════════════════════════════════════════════════════


@productivity_mcp.tool()
async def daily_briefing(agent_id: str, target_date: str = "") -> str:
    """Generate a morning briefing for the user.

    Combines today's calendar events, overdue tasks, tasks due today,
    and free time windows into a single actionable summary.

    Args:
        agent_id: The agent/user ID
        target_date: Date to brief on (YYYY-MM-DD). Defaults to today.
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not target_date:
            target_date = date.today().isoformat()

        next_day = (date.fromisoformat(target_date) + timedelta(days=1)).isoformat()

        # 1. Today's events
        events = await client.list_events(
            creds, calendar_id="primary",
            time_min=_ensure_rfc3339(target_date),
            time_max=_ensure_rfc3339(next_day),
            max_results=50,
        )

        event_summaries = []
        total_meeting_hours = 0.0
        for e in events:
            start = e.get("start", {})
            end = e.get("end", {})
            s_time = start.get("dateTime") or start.get("date", "")
            e_time = end.get("dateTime") or end.get("date", "")
            event_summaries.append({
                "title": e.get("summary", "(No title)"),
                "start": s_time,
                "end": e_time,
                "location": e.get("location"),
            })
            # Estimate meeting hours
            try:
                if start.get("dateTime") and end.get("dateTime"):
                    s_dt = datetime.fromisoformat(start["dateTime"])
                    e_dt = datetime.fromisoformat(end["dateTime"])
                    total_meeting_hours += (e_dt - s_dt).total_seconds() / 3600.0
            except (ValueError, TypeError):
                pass

        # 2. Free/busy analysis
        freebusy = await client.query_free_busy(
            creds,
            time_min=_ensure_rfc3339(target_date),
            time_max=_ensure_rfc3339(next_day),
        )
        busy_count = 0
        for cal_data in freebusy.get("calendars", {}).values():
            busy_count += len(cal_data.get("busy", []))

        # 3. Tasks due today or overdue
        try:
            all_tasks = await client.list_tasks(
                creds, tasklist_id="@default",
                show_completed=False, max_results=100,
            )
        except Exception:
            all_tasks = []

        overdue_tasks = []
        due_today = []
        for t in all_tasks:
            due = t.get("due")
            if due:
                try:
                    due_date = due[:10]  # "YYYY-MM-DD"
                    if due_date < target_date:
                        overdue_tasks.append({
                            "title": t.get("title", ""),
                            "due": due_date,
                        })
                    elif due_date == target_date:
                        due_today.append({
                            "title": t.get("title", ""),
                            "due": due_date,
                        })
                except (ValueError, TypeError):
                    pass

        briefing = {
            "status": "ok",
            "date": target_date,
            "events": {
                "count": len(event_summaries),
                "total_meeting_hours": round(total_meeting_hours, 1),
                "items": event_summaries,
            },
            "tasks": {
                "overdue_count": len(overdue_tasks),
                "due_today_count": len(due_today),
                "overdue": overdue_tasks,
                "due_today": due_today,
            },
            "availability": {
                "busy_blocks": busy_count,
                "free_hours_estimate": round(max(0, 8 - total_meeting_hours), 1),
            },
        }

        return json.dumps(briefing)

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] daily_briefing failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@productivity_mcp.tool()
async def weekly_summary(
    agent_id: str,
    week_start: str = "",
) -> str:
    """Generate a weekly summary of the user's calendar and tasks.

    Shows: total events, meeting hours, busiest day, tasks completed, tasks pending.

    Args:
        agent_id: The agent/user ID
        week_start: Start of the week (YYYY-MM-DD). Defaults to most recent Monday.
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not week_start:
            today = date.today()
            week_start = (today - timedelta(days=today.weekday())).isoformat()

        week_end = (date.fromisoformat(week_start) + timedelta(days=7)).isoformat()

        events = await client.list_events(
            creds, calendar_id="primary",
            time_min=_ensure_rfc3339(week_start),
            time_max=_ensure_rfc3339(week_end),
            max_results=200,
        )

        # Analyze by day
        day_stats: dict[str, dict] = {}
        total_hours = 0.0

        for e in events:
            start = e.get("start", {})
            end = e.get("end", {})
            s_time = start.get("dateTime") or start.get("date", "")
            e_day = s_time[:10] if s_time else "unknown"

            if e_day not in day_stats:
                day_stats[e_day] = {"count": 0, "hours": 0.0}
            day_stats[e_day]["count"] += 1

            try:
                if start.get("dateTime") and end.get("dateTime"):
                    s_dt = datetime.fromisoformat(start["dateTime"])
                    e_dt = datetime.fromisoformat(end["dateTime"])
                    hours = (e_dt - s_dt).total_seconds() / 3600.0
                    day_stats[e_day]["hours"] += hours
                    total_hours += hours
            except (ValueError, TypeError):
                pass

        busiest_day = max(day_stats, key=lambda d: day_stats[d]["count"]) if day_stats else None
        freest_day = min(day_stats, key=lambda d: day_stats[d]["count"]) if day_stats else None

        # Tasks
        try:
            tasks = await client.list_tasks(
                creds, tasklist_id="@default",
                show_completed=True, max_results=200,
            )
        except Exception:
            tasks = []

        completed_this_week = 0
        pending = 0
        for t in tasks:
            if t.get("status") == "completed":
                completed_at = t.get("completed", "")
                if completed_at and completed_at[:10] >= week_start and completed_at[:10] < week_end:
                    completed_this_week += 1
            else:
                pending += 1

        summary = {
            "status": "ok",
            "week": {"start": week_start, "end": week_end},
            "events": {
                "total_events": len(events),
                "total_meeting_hours": round(total_hours, 1),
                "busiest_day": busiest_day,
                "freest_day": freest_day,
                "daily_breakdown": day_stats,
            },
            "tasks": {
                "completed_this_week": completed_this_week,
                "still_pending": pending,
            },
        }

        return json.dumps(summary)

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] weekly_summary failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  TIME MANAGEMENT (2)
# ══════════════════════════════════════════════════════════════


@productivity_mcp.tool()
async def time_block_focus(
    agent_id: str,
    date_str: str = "",
    duration_minutes: str = "120",
    title: str = "Focus Time",
    preferred_time: str = "morning",
    calendar_id: str = "primary",
) -> str:
    """Find a free slot and create a focus time block on the user's calendar.

    Analyzes the user's schedule to find the best uninterrupted block.

    Args:
        agent_id: The agent/user ID
        date_str: Date to block focus time (YYYY-MM-DD). Defaults to tomorrow.
        duration_minutes: How long the focus block should be (default: "120")
        title: Name for the event (default: "Focus Time")
        preferred_time: "morning" (8am-12pm), "afternoon" (12pm-5pm), or "any"
        calendar_id: Calendar to create in
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not date_str:
            date_str = (date.today() + timedelta(days=1)).isoformat()

        try:
            duration = int(duration_minutes)
        except ValueError:
            duration = 120

        next_day = (date.fromisoformat(date_str) + timedelta(days=1)).isoformat()

        # Get free/busy for the day
        freebusy = await client.query_free_busy(
            creds,
            time_min=_ensure_rfc3339(date_str),
            time_max=_ensure_rfc3339(next_day),
        )

        # Collect all busy periods
        busy_periods = []
        for cal_data in freebusy.get("calendars", {}).values():
            for b in cal_data.get("busy", []):
                try:
                    busy_periods.append((
                        datetime.fromisoformat(b["start"]),
                        datetime.fromisoformat(b["end"]),
                    ))
                except (ValueError, KeyError):
                    pass

        busy_periods.sort(key=lambda x: x[0])

        # Define search window based on preference
        target_date = date.fromisoformat(date_str)
        if preferred_time == "morning":
            window_start = datetime(target_date.year, target_date.month, target_date.day, 8, 0, tzinfo=timezone.utc)
            window_end = datetime(target_date.year, target_date.month, target_date.day, 12, 0, tzinfo=timezone.utc)
        elif preferred_time == "afternoon":
            window_start = datetime(target_date.year, target_date.month, target_date.day, 12, 0, tzinfo=timezone.utc)
            window_end = datetime(target_date.year, target_date.month, target_date.day, 17, 0, tzinfo=timezone.utc)
        else:
            window_start = datetime(target_date.year, target_date.month, target_date.day, 8, 0, tzinfo=timezone.utc)
            window_end = datetime(target_date.year, target_date.month, target_date.day, 18, 0, tzinfo=timezone.utc)

        # Find a free slot within the window
        slot_start = window_start
        found_slot = None

        for busy_start, busy_end in busy_periods:
            if busy_start > window_end:
                break
            if busy_end <= slot_start:
                continue

            # Check if there's enough time before this busy period
            available = (min(busy_start, window_end) - slot_start).total_seconds() / 60
            if available >= duration:
                found_slot = (slot_start, slot_start + timedelta(minutes=duration))
                break

            # Move past this busy period
            slot_start = max(slot_start, busy_end)

        # Check remaining time after all busy periods
        if not found_slot and slot_start < window_end:
            available = (window_end - slot_start).total_seconds() / 60
            if available >= duration:
                found_slot = (slot_start, slot_start + timedelta(minutes=duration))

        if not found_slot:
            return json.dumps({
                "status": "no_slot",
                "message": f"No free {duration}-minute slot found on {date_str} during {preferred_time}. Try a different time or shorter duration.",
            })

        # Create the focus event
        event = await client.create_event(
            creds,
            summary=title,
            start=found_slot[0].isoformat(),
            end=found_slot[1].isoformat(),
            calendar_id=calendar_id,
            description="Auto-created focus time block by CoAgent.",
            color_id="7",  # Peacock (blue-green for focus)
        )

        return json.dumps({
            "status": "success",
            "message": f"Focus time '{title}' blocked: {found_slot[0].strftime('%H:%M')} - {found_slot[1].strftime('%H:%M')} on {date_str}",
            "event": {
                "event_id": event.get("id"),
                "title": event.get("summary"),
                "start": found_slot[0].isoformat(),
                "end": found_slot[1].isoformat(),
            },
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] time_block_focus failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@productivity_mcp.tool()
async def schedule_with_preferences(
    agent_id: str,
    title: str,
    duration_minutes: str = "60",
    date_range_start: str = "",
    date_range_end: str = "",
    preferred_time: str = "any",
    avoid_days: str = "",
    buffer_minutes: str = "0",
    calendar_id: str = "primary",
) -> str:
    """Smart self-scheduling — find the best time slot based on user preferences.

    Scans the user's calendar across a date range and finds the optimal slot
    considering their preferences and existing schedule.

    Args:
        agent_id: The agent/user ID
        title: What to schedule
        duration_minutes: How long (default: "60")
        date_range_start: Start searching from (YYYY-MM-DD). Defaults to tomorrow.
        date_range_end: Search until (YYYY-MM-DD). Defaults to 7 days from start.
        preferred_time: "morning" (8-12), "afternoon" (12-17), "evening" (17-21), or "any" (8-21)
        avoid_days: Comma-separated days to avoid, e.g. "Saturday,Sunday"
        buffer_minutes: Minimum buffer before/after existing events (default: "0")
        calendar_id: Calendar to create in
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not date_range_start:
            date_range_start = (date.today() + timedelta(days=1)).isoformat()
        if not date_range_end:
            date_range_end = (date.fromisoformat(date_range_start) + timedelta(days=7)).isoformat()

        try:
            duration = int(duration_minutes)
        except ValueError:
            duration = 60
        try:
            buffer = int(buffer_minutes)
        except ValueError:
            buffer = 0

        avoid_set = set()
        if avoid_days:
            avoid_set = {d.strip().lower() for d in avoid_days.split(",")}

        day_names = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]

        # Define time windows
        time_windows = {
            "morning": (8, 12),
            "afternoon": (12, 17),
            "evening": (17, 21),
            "any": (8, 21),
        }
        win_start_h, win_end_h = time_windows.get(preferred_time, (8, 21))

        # Get free/busy for the whole range
        freebusy = await client.query_free_busy(
            creds,
            time_min=_ensure_rfc3339(date_range_start),
            time_max=_ensure_rfc3339(date_range_end),
        )

        busy_periods = []
        for cal_data in freebusy.get("calendars", {}).values():
            for b in cal_data.get("busy", []):
                try:
                    busy_periods.append((
                        datetime.fromisoformat(b["start"]),
                        datetime.fromisoformat(b["end"]),
                    ))
                except (ValueError, KeyError):
                    pass
        busy_periods.sort(key=lambda x: x[0])

        # Search day by day
        current = date.fromisoformat(date_range_start)
        end = date.fromisoformat(date_range_end)
        found_slot = None

        while current < end:
            # Skip avoided days
            if day_names[current.weekday()] in avoid_set:
                current += timedelta(days=1)
                continue

            day_start = datetime(current.year, current.month, current.day, win_start_h, 0, tzinfo=timezone.utc)
            day_end = datetime(current.year, current.month, current.day, win_end_h, 0, tzinfo=timezone.utc)

            # Filter busy periods for this day
            day_busy = [
                (max(bs, day_start), min(be, day_end))
                for bs, be in busy_periods
                if bs < day_end and be > day_start
            ]
            day_busy.sort(key=lambda x: x[0])

            slot_start = day_start

            for bs, be in day_busy:
                # Apply buffer
                effective_busy_start = bs - timedelta(minutes=buffer)
                available = (effective_busy_start - slot_start).total_seconds() / 60

                if available >= duration:
                    found_slot = (slot_start, slot_start + timedelta(minutes=duration))
                    break

                slot_start = be + timedelta(minutes=buffer)

            if found_slot:
                break

            # Check after last busy period
            if not found_slot and slot_start < day_end:
                available = (day_end - slot_start).total_seconds() / 60
                if available >= duration:
                    found_slot = (slot_start, slot_start + timedelta(minutes=duration))
                    break

            current += timedelta(days=1)

        if not found_slot:
            return json.dumps({
                "status": "no_slot",
                "message": f"No {duration}-minute slot found between {date_range_start} and {date_range_end} with your preferences.",
                "suggestions": "Try widening the date range, choosing a different time preference, or reducing the duration.",
            })

        # Create the event
        event = await client.create_event(
            creds,
            summary=title,
            start=found_slot[0].isoformat(),
            end=found_slot[1].isoformat(),
            calendar_id=calendar_id,
            description=f"Scheduled by CoAgent with preferences: {preferred_time}" + (f", avoiding {avoid_days}" if avoid_days else ""),
        )

        return json.dumps({
            "status": "success",
            "message": f"'{title}' scheduled for {found_slot[0].strftime('%A %B %d, %H:%M')} - {found_slot[1].strftime('%H:%M')}",
            "event": {
                "event_id": event.get("id"),
                "title": title,
                "start": found_slot[0].isoformat(),
                "end": found_slot[1].isoformat(),
                "day": found_slot[0].strftime("%A"),
            },
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] schedule_with_preferences failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


# ══════════════════════════════════════════════════════════════
#  DAILY LIFE & REMINDERS (3)
# ══════════════════════════════════════════════════════════════


@productivity_mcp.tool()
async def set_event_reminders(
    agent_id: str,
    event_id: str,
    reminder_minutes: str,
    calendar_id: str = "primary",
) -> str:
    """Set custom reminders for a calendar event.

    Args:
        agent_id: The agent/user ID
        event_id: Event to set reminders on
        reminder_minutes: Comma-separated minutes before event, e.g. "10,30,60" for reminders at 10min, 30min, 1hr before
        calendar_id: Calendar containing the event
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        mins = [int(m.strip()) for m in reminder_minutes.split(",")]
        reminders = {
            "useDefault": False,
            "overrides": [{"method": "popup", "minutes": m} for m in mins],
        }

        event = await client.update_event(
            creds, event_id,
            updates={"reminders": reminders},
            calendar_id=calendar_id,
        )

        return json.dumps({
            "status": "success",
            "message": f"Reminders set: {', '.join(str(m) + ' min' for m in mins)} before the event.",
            "event_id": event.get("id"),
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] set_event_reminders failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@productivity_mcp.tool()
async def create_recurring_routine(
    agent_id: str,
    title: str,
    time: str,
    duration_minutes: str = "60",
    days: str = "Monday,Tuesday,Wednesday,Thursday,Friday",
    end_date: str = "",
    calendar_id: str = "primary",
    color_id: str = "",
    reminder_minutes: str = "10",
) -> str:
    """Create a recurring routine on the user's calendar.

    Perfect for habits, workouts, standups, or any regular activity.

    Examples:
      - "Gym" at 06:00, days="Monday,Wednesday,Friday"
      - "Morning Journaling" at 07:00, days="Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday"
      - "Team Standup" at 09:00, days="Monday,Tuesday,Wednesday,Thursday,Friday"

    Args:
        agent_id: The agent/user ID
        title: Routine name
        time: Start time (HH:MM in 24-hour format, e.g. "06:00", "14:30")
        duration_minutes: How long each session is (default: "60")
        days: Comma-separated days, e.g. "Monday,Wednesday,Friday"
        end_date: When to stop the recurrence (YYYY-MM-DD). Leave empty for indefinite.
        calendar_id: Calendar to create in
        color_id: Event color (1-11)
        reminder_minutes: Reminder before each occurrence (default: "10")
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        try:
            duration = int(duration_minutes)
        except ValueError:
            duration = 60

        # Build RRULE
        day_map = {
            "monday": "MO", "tuesday": "TU", "wednesday": "WE",
            "thursday": "TH", "friday": "FR", "saturday": "SA", "sunday": "SU",
        }
        rrule_days = []
        for d in days.split(","):
            d_clean = d.strip().lower()
            if d_clean in day_map:
                rrule_days.append(day_map[d_clean])

        if not rrule_days:
            return json.dumps({"status": "error", "message": "No valid days provided."})

        rrule = f"RRULE:FREQ=WEEKLY;BYDAY={','.join(rrule_days)}"
        if end_date:
            rrule += f";UNTIL={end_date.replace('-', '')}T235959Z"

        # Build start/end times for the first occurrence
        # Use next occurrence of the first day
        today = date.today()
        target_weekday = list(day_map.keys()).index(days.split(",")[0].strip().lower())
        days_until = (target_weekday - today.weekday()) % 7
        if days_until == 0:
            days_until = 7  # Start next week if today
        first_date = today + timedelta(days=days_until)

        start_dt = f"{first_date.isoformat()}T{time}:00"
        hour, minute = map(int, time.split(":"))
        end_dt_obj = datetime(first_date.year, first_date.month, first_date.day, hour, minute) + timedelta(minutes=duration)
        end_dt = f"{first_date.isoformat()}T{end_dt_obj.strftime('%H:%M')}:00"

        # Build reminders
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
            start=start_dt,
            end=end_dt,
            calendar_id=calendar_id,
            recurrence=[rrule],
            color_id=color_id or None,
            reminders=reminders,
            description=f"Recurring routine: {days}",
        )

        return json.dumps({
            "status": "success",
            "message": f"Routine '{title}' created: {time} on {days}" + (f" until {end_date}" if end_date else " (ongoing)"),
            "event": {
                "event_id": event.get("id"),
                "title": event.get("summary"),
                "first_occurrence": start_dt,
                "recurrence": rrule,
            },
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] create_recurring_routine failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})


@productivity_mcp.tool()
async def get_agenda(
    agent_id: str,
    target_date: str = "",
) -> str:
    """Get a formatted agenda for a specific day with tasks interleaved.

    Combines calendar events and tasks due that day into a timeline.

    Args:
        agent_id: The agent/user ID
        target_date: Date to show agenda for (YYYY-MM-DD). Defaults to today.
    """
    try:
        creds = await _get_creds(agent_id)
        client = _get_client()

        if not target_date:
            target_date = date.today().isoformat()

        next_day = (date.fromisoformat(target_date) + timedelta(days=1)).isoformat()

        # Events for the day
        events = await client.list_events(
            creds, calendar_id="primary",
            time_min=_ensure_rfc3339(target_date),
            time_max=_ensure_rfc3339(next_day),
            max_results=50,
        )

        # Tasks due that day
        try:
            all_tasks = await client.list_tasks(
                creds, tasklist_id="@default",
                show_completed=False, max_results=100,
            )
        except Exception:
            all_tasks = []

        tasks_today = []
        for t in all_tasks:
            due = t.get("due", "")
            if due and due[:10] == target_date:
                tasks_today.append({
                    "type": "task",
                    "title": t.get("title", ""),
                    "notes": t.get("notes"),
                    "task_id": t.get("id"),
                    "status": t.get("status"),
                })

        # Build timeline
        agenda_items = []
        for e in events:
            start = e.get("start", {})
            end_info = e.get("end", {})
            agenda_items.append({
                "type": "event",
                "title": e.get("summary", "(No title)"),
                "start": start.get("dateTime") or start.get("date", ""),
                "end": end_info.get("dateTime") or end_info.get("date", ""),
                "location": e.get("location"),
                "event_id": e.get("id"),
            })

        return json.dumps({
            "status": "ok",
            "date": target_date,
            "event_count": len(agenda_items),
            "task_count": len(tasks_today),
            "events": agenda_items,
            "tasks_due": tasks_today,
        })

    except RuntimeError as e:
        return json.dumps({"status": "auth_required", "message": str(e)})
    except Exception as e:
        logger.error(f"[MCP] get_agenda failed: {e}", exc_info=True)
        return json.dumps({"status": "error", "message": str(e)})
