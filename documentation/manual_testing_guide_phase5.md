# CoAgent4U — Manual Testing Guide (Phase 5)

> **Base URL (Production):** `https://api.coagent4u.com`
> **Base URL (Local):** `http://localhost:8080`
>
> All examples below use the **production** URL. Replace with `http://localhost:8080` when testing locally.

---

## 1. Prerequisites

### Build & Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run -pl coagent-app
```

### Cookie Handling with `curl`

| Flag | Purpose |
|------|---------|
| `-c cookies.txt` | **Write** cookies received from the server into `cookies.txt` |
| `-b cookies.txt` | **Read** cookies from `cookies.txt` and send them with the request |

> [!TIP]
> Always use `-c cookies.txt` on login/registration calls and `-b cookies.txt` on subsequent calls to maintain the `coagent_session` JWT.

### Slack App Configuration

| Setting | Value |
|---------|-------|
| **Bot Token Scopes** | `chat:write`, `im:history`, `app_mentions:read` |
| **User Token Scopes** | `openid`, `profile`, `email` |
| **Event Subscriptions URL** | `https://api.coagent4u.com/slack/events` |
| **Interactivity Request URL** | `https://api.coagent4u.com/slack/interactions` |
| **OAuth Redirect URL** | `https://api.coagent4u.com/auth/slack/callback` |
| **Bot Events** | `app_mention`, `message.im` |


## 2. Testing Slack OAuth with `curl`

> [!IMPORTANT]
> The Slack OAuth step **requires a browser** — you cannot complete it with `curl` alone. After the browser step, use `curl` for everything else.

### Step 1: Complete OAuth in Browser

1. Open in your browser:
   ```
   https://api.coagent4u.com/auth/slack/start
   ```
2. Sign in to Slack → consent → Slack redirects to `/auth/slack/callback`
3. Backend processes it → sets `coagent_session` cookie → redirects you
4. Open **browser Dev Tools → Application → Cookies** and copy the `coagent_session` value

### Step 2: Use the Cookie in `curl`

```bash
# Option A: Pass cookie directly
curl -b "coagent_session=<PASTE_JWT_HERE>" https://api.coagent4u.com/auth/session

# Option B: Save to file for reuse
echo "api.coagent4u.com	FALSE	/	TRUE	0	coagent_session	<PASTE_JWT_HERE>" > cookies.txt
curl -b cookies.txt https://api.coagent4u.com/auth/session
```

### If New User — Submit Username

```bash
curl -i -X POST https://api.coagent4u.com/auth/username \
  -H "Content-Type: application/json" \
  -b "coagent_session=<PENDING_JWT>" \
  -c cookies.txt \
  -d '{"username": "tanmay"}'
```

### If Existing User — Already Logged In

```bash
# Session check
curl -b "coagent_session=<FULL_JWT>" https://api.coagent4u.com/auth/session

# Profile
curl -b "coagent_session=<FULL_JWT>" https://api.coagent4u.com/me
```

> [!NOTE]
> `curl -L -c cookies.txt https://api.coagent4u.com/auth/slack/start` will follow the redirect but hit Slack's HTML login page — it won't complete the OAuth flow.

---

## 3. Health Check

```bash
curl https://api.coagent4u.com/api/health
```

*Expected:* `OK`

---

## 4. Slack Onboarding Flow

This is the primary user journey. Users sign in via Slack. The backend checks if the user exists — if yes, they're logged in directly; if no, they're prompted for a username and then registered.

### 4.1 Initiate Slack Login

Open in a browser to go through the full flow:

```
https://api.coagent4u.com/auth/slack/start
```

Or verify the redirect via curl:

```bash
curl -i -c cookies.txt https://api.coagent4u.com/auth/slack/start
```

*Expected:* `HTTP 302 Found` → `Location: https://slack.com/openid/connect/authorize?...`

### 4.2 Slack OAuth Callback (Automatic)

After Slack consent, Slack redirects to `GET /auth/slack/callback?code=xxx`. The backend:

1. Exchanges the code for Slack identity (`slackUserId`, `workspaceId`, `email`, `displayName`)
2. Looks up the user by `slackUserId + workspaceId`

**Path A — Existing User (Login):**
- Issues a full JWT with `pendingRegistration=false`
- `302` → `/dashboard` + `Set-Cookie: coagent_session=<fullJWT>`
- ✅ User is logged in — no further steps needed

**Path B — New User (Onboarding):**
- Issues a pending JWT (10-min expiry) with Slack identity embedded as signed claims
- `302` → `/onboarding` (no query params — identity is in the JWT)
- ⏳ User must submit a username within 10 minutes

### 4.3 Check Session Status

```bash
curl -b cookies.txt https://api.coagent4u.com/auth/session
```

**After Path A (existing user):**
```json
{"authenticated":true, "username":"tanmay", "pendingRegistration":false}
```

**After Path B (new user, before username):**
```json
{"authenticated":true, "username":"", "pendingRegistration":true}
```

**No cookie:**
```json
{"authenticated":false}
```

### 4.4 Submit Username (New Users Only)

```bash
curl -i -X POST https://api.coagent4u.com/auth/username \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -c cookies.txt \
  -d '{"username": "tanmay"}'
```

> [!CAUTION]
> The body accepts **only** `{"username": "..."}`. Slack identity is read from the signed JWT — it cannot be tampered with.

*Expected (success):*
```json
{"success":true, "username":"tanmay"}
```
+ `Set-Cookie: coagent_session=<fullJWT>`

This also **automatically provisions an AI Agent** for the user.

**Error responses:**

| Status | Body | Cause |
|--------|------|-------|
| 400 | `{"error": "Username must match ^[a-zA-Z0-9_-]{3,32}$"}` | Invalid format |
| 400 | `{"error": "Username already taken: ..."}` | Duplicate |
| 400 | `{"error": "User already registered"}` | JWT is not pending |
| 400 | `{"error": "Missing Slack identity in session..."}` | Corrupted JWT |
| 401 | `{"error": "Invalid or expired session"}` | Pending JWT expired (10 min) |

### 4.5 Verify Full Access (Post-Login / Post-Registration)

```bash
# Session
curl -b cookies.txt https://api.coagent4u.com/auth/session

# Profile
curl -b cookies.txt https://api.coagent4u.com/me
```

*Expected (profile):*
```json
{"username":"tanmay", "email":"...", "googleCalendarConnected":false, "createdAt":"..."}
```

### 4.6 Logout

```bash
curl -X POST -b cookies.txt https://api.coagent4u.com/auth/logout
```

*Expected:* `{"success":true, "message":"Logged out successfully"}` + `Set-Cookie: coagent_session=; Max-Age=0`

---

## 5. Onboarding Security Tests

### 5.1 Pending JWT Expiry

1. Complete Slack OAuth for a new user (do NOT submit username)
2. Wait **11 minutes**
3. Submit username:
   ```bash
   curl -i -X POST https://api.coagent4u.com/auth/username \
     -H "Content-Type: application/json" \
     -b cookies.txt -c cookies.txt \
     -d '{"username": "toolate"}'
   ```
   *Expected:* `401` — pending JWT expired

### 5.2 Identity Tamper-Proofing

```bash
curl -i -X POST https://api.coagent4u.com/auth/username \
  -H "Content-Type: application/json" \
  -b cookies.txt -c cookies.txt \
  -d '{"username": "hacker", "slackUserId": "U_VICTIM", "workspaceId": "T_VICTIM"}'
```

*Expected:* `200 OK` — user is registered with Slack identity from the JWT, extra fields in body are **ignored**.

### 5.3 Username Collision

```bash
curl -i -X POST https://api.coagent4u.com/auth/username \
  -H "Content-Type: application/json" \
  -b cookies.txt -c cookies.txt \
  -d '{"username": "tanmay"}'
```

*Expected:* `400` with `{"error": "Username already taken: tanmay"}`

### 5.4 Agent Uniqueness

After onboarding, verify exactly 1 agent per user:

```sql
SELECT COUNT(*) FROM agents WHERE user_id = '<userId>';
-- Expected: 1
```

---

## 6. Register Test Users (Direct API)

For testing other features without going through Slack OAuth:

```bash
# Register Alice
curl -X POST https://api.coagent4u.com/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "slackUserId": "U_ALICE", "workspaceId": "T_LOCAL"}'

# Provision Agent for Alice
curl -X POST https://api.coagent4u.com/api/agents \
  -H "Content-Type: application/json" \
  -d '{"userId": "<ALICE_USER_UUID>"}'

# Lookup User
curl https://api.coagent4u.com/api/users/<userId>
```

---

## 7. Intent Parsing (Sandbox)

```bash
# Tier 1 (regex): "view schedule"
curl -X POST https://api.coagent4u.com/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "show me my schedule for tomorrow", "forceLlm": false}'

# Tier 1 (regex): "schedule with"
curl -X POST https://api.coagent4u.com/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "schedule a meeting with @bob", "forceLlm": false}'

# Force Tier 2 (LLM)
curl -X POST https://api.coagent4u.com/api/sandbox/parse-intent \
  -H "Content-Type: application/json" \
  -d '{"text": "I need to talk to alice next week", "forceLlm": true}'
```

*Expected:* JSON with `tier1Type`, `tier2Type`, `finalDecision`, `debugStatus`.

---

## 8. Google Calendar Integration

> [!NOTE]
> Requires a valid `coagent_session` cookie (logged-in user).

### 8.1 Start Google OAuth
```bash
curl -i -b cookies.txt https://api.coagent4u.com/integrations/google/authorize
```

### 8.2 Check Connection Status
```bash
curl -b cookies.txt https://api.coagent4u.com/integrations/google/status
```

### 8.3 Disconnect
```bash
curl -X DELETE -b cookies.txt https://api.coagent4u.com/integrations/google/disconnect
```

---

## 9. Slack Webhooks (Simulation)

> [!IMPORTANT]
> The `SLACK_SIGNING_SECRET` in the script must match `application.properties`.

```bash
# Simulate DMs
python scripts/simulate_slack.py event "@CoAgent view schedule"
python scripts/simulate_slack.py event "@CoAgent schedule with @bob"

# Simulate button clicks (after approval is created)
python scripts/simulate_slack.py interaction approve_action <approval-uuid>
python scripts/simulate_slack.py interaction reject_action <approval-uuid>
```

---

## 10. Verification Checkpoints

| What to check | How |
|---|---|
| App logs | Console: `Slack message sent`, `Created approval`, state transitions |
| Database | `SELECT * FROM approvals;` / `SELECT * FROM coordinations;` on NeonDB |
| User lookup | `curl https://api.coagent4u.com/api/users/<userId>` |
| Session | `curl -b cookies.txt https://api.coagent4u.com/auth/session` |
| Google status | `curl -b cookies.txt https://api.coagent4u.com/integrations/google/status` |
| Agent count | `SELECT COUNT(*) FROM agents WHERE user_id = '<userId>';` → exactly 1 |

---

## Complete API Reference

### Public Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/health` | Health check |
| `POST` | `/api/users` | Register user (direct API) |
| `GET` | `/api/users/{userId}` | Get user profile |
| `POST` | `/api/agents` | Provision agent |
| `POST` | `/api/sandbox/parse-intent` | Test intent parsing |

### Auth Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/auth/slack/start` | Redirect to Slack OAuth |
| `GET` | `/auth/slack/callback` | Slack OAuth callback |
| `POST` | `/auth/username` | Submit username — body: `{"username":"..."}` only |
| `GET` | `/auth/session` | Session status |
| `POST` | `/auth/logout` | Logout |

### Authenticated Endpoints (require `coagent_session` cookie)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/me` | User profile |
| `GET` | `/integrations/google/authorize` | Start Google OAuth |
| `GET` | `/integrations/google/callback` | Google OAuth callback |
| `GET` | `/integrations/google/status` | Google Calendar status |
| `DELETE` | `/integrations/google/disconnect` | Disconnect Google Calendar |

### Slack Webhook Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/slack/events` | Events API |
| `POST` | `/slack/interactions` | Interactive button callbacks |
