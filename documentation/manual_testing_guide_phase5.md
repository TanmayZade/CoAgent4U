# Phase 5 Manual Testing Guide

## 1. Prerequisites

App must be running:
```bash
mvn clean install -DskipTests

mvn spring-boot:run -pl coagent-app
```

-c cookies.txt: Tells curl to "write" any cookies received from the server into this file.
-b cookies.txt: Tells curl to "read" cookies from this file and send them with the request.


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

```
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "tanmay", "email": "tanmay@example.com", "slackUserId": "U0AEA4CJM42", "workspaceId": "T0AEP1ZHEKT"}'

#Agent provisioning
curl -X POST http://localhost:8080/api/agents \
  -H "Content-Type: application/json" \
  -d '{"userId": "38170dc0-6b9a-4e00-81dd-331325790255"}'

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

---

## 8. Test Auth, Session & Integration Endpoints

> [!NOTE]  
> Endpoints that require authentication (`/me`, `/integrations/google/*`, `/auth/username`, `/auth/logout`) require a valid `coagent_session` cookie (JWT). You either need to log in via the browser first and copy the cookie value, or use a tool like Postman which handles cookies automatically.

### Test Slack Login Redirect (Public)
```bash
curl -i http://localhost:8080/auth/slack/start
```
*Expected: `HTTP 302 Found` with a `Location` header pointing to `https://slack.com/openid/connect/authorize...`*

### Test Session Status (Unauthenticated)
```bash
curl http://localhost:8080/auth/session
```
*Expected: `{"authenticated":false}`*

### Test Username Registration (Requires Pending JWT)
Replace `<PENDING_JWT>` with the cookie value received after a new Slack login flow.
```bash
curl -i -X POST http://localhost:8080/auth/username \
  -H "Content-Type: application/json" \
  -H "Cookie: coagent_session=eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZjhkOTkwZS0yMzkyLTQ0NGUtOWFhNS1kZDllMTM0MGFkNWQiLCJzdWIiOiI1OWRhNmViYS1hNmJmLTQzOTktOGJjMS1iNTQ3YTBjNjIwZjEiLCJwZW5kaW5nX3JlZ2lzdHJhdGlvbiI6dHJ1ZSwiaWF0IjoxNzczMTM4NDQ0LCJleHAiOjE3NzMyMjQ4NDR9.baceemwv-zWmbjG1qPiDcmJPdmWYYK9mIwh4gwQJdbc" \
  -c cookies.txt \
  -d '{"username": "testuser", "slackUserId": "U12345", "workspaceId": "T12345"}'
```
*Expected: `HTTP 200 OK` and a new `Set-Cookie: coagent_session=...` containing the full JWT.*

### Test Session Status (Authenticated)
Replace `<VALID_JWT>` with a fully registered JWT, or use the saved `cookies.txt`.
```bash
# Option A: Manual Header
curl http://localhost:8080/auth/session \
  -H "Cookie: coagent_session=<VALID_JWT>"

# Option B: Using Cookie File
curl -b cookies.txt http://localhost:8080/auth/session
```
*Expected: `{"authenticated":true, "userId":"...", "username":"testuser", "pendingRegistration":false}`*

### Test User Profile
```bash
# Option A: Manual Header
curl http://localhost:8080/me \
  -H "Cookie: coagent_session=<VALID_JWT>"

# Option B: Using Cookie File
curl -b cookies.txt http://localhost:8080/me
```
*Expected: User profile JSON including `googleCalendarConnected` status.*

### Test Google Authorization Redirect
```bash
# Option A: Manual Header
curl -i http://localhost:8080/integrations/google/authorize \
  -H "Cookie: coagent_session=<VALID_JWT>"

# Option B: Using Cookie File
curl -i -b cookies.txt http://localhost:8080/integrations/google/authorize
```
*Expected: `HTTP 302 Found` with `Location` header pointing to `accounts.google.com` including a signed `state` JWT.*

### Test Google Disconnect
```bash
# Option A: Manual Header
curl -X DELETE http://localhost:8080/integrations/google/disconnect \
  -H "Cookie: coagent_session=<VALID_JWT>"

# Option B: Using Cookie File
curl -X DELETE -b cookies.txt http://localhost:8080/integrations/google/disconnect
```

### Test Logout
```bash
# Option A: Manual Header
curl -X POST http://localhost:8080/auth/logout \
  -H "Cookie: coagent_session=<VALID_JWT>"

# Option B: Using Cookie File
curl -X POST -b cookies.txt http://localhost:8080/auth/logout
```
*Expected: `HTTP 200 OK` returning `{"success":true,"message":"Logged out successfully"}` and a `Set-Cookie: coagent_session=; Max-Age=0` header.*

