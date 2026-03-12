# CoAgent4U — Slack Messaging Guide

> This document describes all messages a user can send to CoAgent via Slack, their expected formats, and how the system interprets them.

---

## How It Works

Users interact with CoAgent by sending messages in Slack. There are two ways:

| Method | How | Trigger |
|--------|-----|---------|
| **Direct Message (DM)** | Send a message in a DM with the CoAgent bot | `message` event |
| **App Mention** | Mention `@CoAgent` in any channel the bot is in | `app_mention` event |

The backend processes messages through a **2-tier intent parser**:

1. **Tier 1 (Regex)** — Fast pattern matching (instant, no API calls)
2. **Tier 2 (LLM Fallback)** — If Tier 1 doesn't match, the LLM classifies the intent

---

## Supported Intents

### 1. View Schedule

**Purpose:** Show the user's upcoming calendar events.

**Example messages:**
```
show my schedule
view my calendar
what's on my agenda
check my events
display my appointments
```

**Trigger words:** `show`, `view`, `display`, `what's`, `check` + `schedule`, `calendar`, `agenda`, `events`, `appointments`

**Parsed as:** `VIEW_SCHEDULE` (no parameters)

---

### 2. Schedule Meeting With Someone

**Purpose:** Initiate a coordinated meeting with another user.

**Example messages:**
```
schedule a meeting with @bob
arrange a call with @alice
set up a session with @tanmay
coordinate a meeting with @bob tomorrow at 3pm
book a call with @alice next Monday
```

**Trigger words:** `schedule`, `arrange`, `set up`, `coordinate`, `book` + `with` + `@username`

**Parsed as:** `SCHEDULE_WITH`

| Parameter | Description | Example |
|-----------|-------------|---------|
| `targetUser` | The mentioned user's Slack ID or username | `bob`, `slack:U12345` |
| `dateTime` | Optional time/date context | `tomorrow at 3pm` |

> [!NOTE]
> In DMs, mention the target user with `@username`. In channels, the first `@CoAgent` mention is stripped — subsequent `@user` mentions are treated as the target.

---

### 3. Add / Create Event

**Purpose:** Create a personal calendar event.

**Example messages:**
```
add a meeting called "Team Standup" on Monday at 9am
create an event "Lunch with client" at 12:30pm tomorrow
schedule appointment "Doctor Visit" on March 15 at 2pm
book a call named "1:1 Review" for Friday at 4pm
set up a session called "Sprint Planning" on next Tuesday
```

**Trigger words:** `add`, `create`, `schedule`, `book`, `set up` + event title + `on`/`at`/`for` + date/time

**Parsed as:** `ADD_EVENT`

| Parameter | Description | Example |
|-----------|-------------|---------|
| `title` | Event title (can be quoted or unquoted) | `Team Standup` |
| `dateTime` | When the event should be | `Monday at 9am` |

---

### 4. Cancel / Delete Event

**Purpose:** Cancel an existing calendar event.

**Example messages:**
```
cancel the meeting called "Team Standup"
delete the event "Lunch with client"
remove appointment "Doctor Visit"
clear the call "1:1 Review"
```

**Trigger words:** `cancel`, `delete`, `remove`, `clear` + event title

**Parsed as:** `CANCEL_EVENT`

| Parameter | Description | Example |
|-----------|-------------|---------|
| `title` | Event title to cancel | `Team Standup` |

---

### 5. Unknown / Ambiguous (LLM Fallback)

If the message doesn't match any Tier 1 pattern, it's sent to the **LLM (Tier 2)** for classification.

**Example messages that go to LLM:**
```
I need to talk to alice next week
Can you find me a free slot tomorrow?
What meetings do I have?
Help me plan a team sync
```

**Parsed as:** `UNKNOWN` by Tier 1 → forwarded to LLM for classification

---

## Message Flow Diagram

```
User sends Slack message
        │
        ▼
┌─────────────────────┐
│ SlackInboundAdapter  │  ← Verifies Slack signature
│ (acknowledge in 3s)  │  ← Deduplicates by event_id
└────────┬────────────┘
         │ (async)
         ▼
┌─────────────────────┐
│ IntentParser (Tier1) │  ← Regex pattern matching
└────────┬────────────┘
         │
    ┌────┴────────┐
    │   Matched?  │
    └────┬────────┘
     Yes │         No
         │          │
         ▼          ▼
  ┌──────────┐  ┌───────────┐
  │ Execute  │  │ LLM Tier2 │
  │ Intent   │  │ Classify  │
  └──────────┘  └───────────┘
```

---

## Testing Messages

### Via Slack (Production)

1. Open a DM with the CoAgent bot
2. Send: `show my schedule`
3. Check app logs for intent parsing output

### Via Sandbox API (curl)

```bash
# Test Tier 1 (regex)
curl -X POST https://api.coagent4u.com/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "show me my schedule for tomorrow", "forceLlm": false}'

# Test Tier 2 (LLM)
curl -X POST https://api.coagent4u.com/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "I need to talk to alice next week", "forceLlm": true}'
```

### Via Slack Simulator Script

```bash
python scripts/simulate_slack.py event "@CoAgent view schedule"
python scripts/simulate_slack.py event "@CoAgent schedule with @bob"
```
