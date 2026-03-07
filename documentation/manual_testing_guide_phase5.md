# Phase 5 Manual Testing Guide

## 1. Prerequisites

App must be running:
```bash
mvn spring-boot:run -pl coagent-app
```

---

## 2. Register Test Users

The endpoint is `POST /api/users` (not `/api/users/register`):

```bash
# Register Alice
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "slackUserId": "U_ALICE", "workspaceId": "T_LOCAL"}'

# Register Bob
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "bob", "email": "bob@example.com", "slackUserId": "U_BOB", "workspaceId": "T_LOCAL"}'
```

Expected: `User registered successfully`

---

## 3. Verify Health

```bash
curl http://localhost:8080/api/health
```
Expected: `OK`

---

## 4. Test Intent Parsing (Sandbox)

```bash
# Test "view schedule" intent
curl -X POST http://localhost:8080/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "show me my schedule for tomorrow", "forceLlm": false}'

# Test "schedule with" intent
curl -X POST http://localhost:8080/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "schedule a meeting with @bob", "forceLlm": false}'
```

---

## 5. Test Slack Event Webhook

Use the simulation script (`pip install requests` required):

```bash
# Simulate a "view schedule" message
python scripts/simulate_slack.py event "@CoAgent view schedule"

# Simulate a "schedule with" message
python scripts/simulate_slack.py event "@CoAgent schedule with @bob"
```

> [!NOTE]
> The `SLACK_SIGNING_SECRET` in the script must match your [application.properties](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.properties). Currently both use `gMy4gslsG6Jq++FidPymUXYm7s8eL/Ygw882sJVFRvI=`. Update the script's `SLACK_SIGNING_SECRET` variable to match.

---

## 6. Test Interactive Approval (Button Clicks)

After a coordination creates a pending approval (check app logs for the approval UUID):

```bash
# Simulate clicking "Approve"
python scripts/simulate_slack.py interaction approve_action <approval-uuid>

# Simulate clicking "Reject"
python scripts/simulate_slack.py interaction reject_action <approval-uuid>
```

---

## 7. Verification Checkpoints

| What to check | How |
|---|---|
| App logs | Watch console for `Slack message sent`, `Created approval`, state transitions |
| Database | Query `SELECT * FROM approvals;` and `SELECT * FROM coordinations;` on NeonDB |
| Users created | `curl http://localhost:8080/api/users/<userId>` |

---

## Available API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users` | Register a new user |
| `GET` | `/api/users/{userId}` | Get user profile |
| `GET` | `/api/health` | Health check |
| `POST` | `/api/sandbox/parse-intent` | Test intent parsing |
| `GET` | `/api/oauth2/authorize?userId=` | Start Google OAuth flow |
| `GET` | `/api/oauth2/callback` | Google OAuth callback |
| `POST` | `/slack/events` | Slack Events API webhook |
| `POST` | `/slack/interactions` | Slack interactive button callbacks |
