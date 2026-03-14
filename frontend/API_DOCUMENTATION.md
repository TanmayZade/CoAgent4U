# CoAgent4U — Complete API Documentation

> **Last Updated:** 2026-03-14  
> **Base URL:** `https://api.coagent4u.com` (production) · `http://localhost:8080` (local dev)

---

## Table of Contents

1. [Authentication Endpoints](#1-authentication-endpoints)
2. [User Profile Endpoints](#2-user-profile-endpoints)
3. [Google Calendar Integration Endpoints](#3-google-calendar-integration-endpoints)
4. [Admin / Testing Endpoints](#4-admin--testing-endpoints)
5. [Slack Webhook Endpoints](#5-slack-webhook-endpoints)
6. [Cross-Cutting Concerns](#6-cross-cutting-concerns)
7. [Error Handling](#7-error-handling)

---

## 1. Authentication Endpoints

**Controller:** `AuthController` — prefix `/auth`

### 1.1 Slack OAuth Login Start

| | |
|---|---|
| **Endpoint** | `GET /auth/slack/start` |
| **Auth** | None (public) |
| **Purpose** | Redirects user to Slack OpenID Connect consent screen |

**Scopes Requested:** `openid`, `profile`, `email`

**Response:**
- `302 Found` → Redirects to `https://slack.com/openid/connect/authorize?...`

---

### 1.2 Slack OAuth Callback

| | |
|---|---|
| **Endpoint** | `GET /auth/slack/callback` |
| **Auth** | None (public) |
| **Purpose** | Handles Slack OAuth callback; creates session |

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | No | Authorization code from Slack |
| `error` | string | No | Error from Slack if user denied |
| `state` | string | No | State param for CSRF validation |

**Sets:**
- HTTP-only cookie: `coagent_session` (JWT, 24h TTL, `SameSite=None`, `Domain=.coagent4u.com`)

**Redirects:**
- Existing user → `{FRONTEND_URL}/dashboard`
- New user → `{FRONTEND_URL}/onboarding` (JWT has `pending_registration=true`)
- Error → `{FRONTEND_URL}/signin?error={reason}`

---

### 1.3 Slack App Install Start

| | |
|---|---|
| **Endpoint** | `GET /auth/slack/install/start` |
| **Auth** | None (public) |
| **Purpose** | Redirects to Slack App installation screen for workspace admins |

**Bot Scopes:** `chat:write`, `chat:write.public`, `commands`  
**User Scopes:** `identity.basic`, `identity.email`, `identity.avatar`

**Response:**
- `302 Found` → Redirects to `https://slack.com/oauth/v2/authorize?...`

---

### 1.4 Slack App Install Callback

| | |
|---|---|
| **Endpoint** | `GET /auth/slack/install/callback` |
| **Auth** | None (public) |
| **Purpose** | Handles bot installation; stores bot token in `workspace_installations` |

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | No | Authorization code from Slack |
| `error` | string | No | Error from Slack |

**Behavior:**
- Exchanges code for bot token via `slackOAuthPort.exchangeForBotToken()`
- Stores `WorkspaceInstallation` (workspaceId, botToken, installerUserId, active=true)
- Sends welcome message to installer via DM

**Redirects:**
- Success → `{FRONTEND_URL}/dashboard?installed=true`
- Error → `{FRONTEND_URL}/dashboard?error={reason}`

---

### 1.5 Submit Username (Onboarding Completion)

| | |
|---|---|
| **Endpoint** | `POST /auth/username` |
| **Auth** | `coagent_session` cookie (must have `pending_registration=true`) |
| **Purpose** | Completes user registration during onboarding |

**Request Body:**
```json
{
  "username": "string"
}
```

**Validation:**
- Pattern: `^[a-zA-Z0-9_-]{3,32}$`
- Username stored lowercase

**Behavior:**
1. Validates pending registration state from JWT
2. Extracts Slack identity from trusted JWT claims (not from request body)
3. Creates User via `RegisterUserUseCase`
4. Provisions Agent (idempotent)
5. Blacklists old pending JWT
6. Issues new JWT with full claims
7. Sends welcome message if workspace has Slack app installed

**Response (200):**
```json
{
  "success": true,
  "username": "john-agent"
}
```
*Also sets updated `coagent_session` cookie*

**Error (400):**
```json
{ "error": "Username must match ^[a-zA-Z0-9_-]{3,32}$" }
```

**Error (400 — Already Registered):**
```json
{ "error": "User already registered" }
```

---

### 1.6 Check Session

| | |
|---|---|
| **Endpoint** | `GET /auth/session` |
| **Auth** | `coagent_session` cookie (optional) |
| **Purpose** | Check current session status |

**Response (200 — Authenticated):**
```json
{
  "authenticated": true,
  "pendingRegistration": false
}
```

**Response (200 — Not Authenticated):**
```json
{
  "authenticated": false
}
```

**Frontend Routing Rules:**
- `authenticated: false` → redirect to `/signin`
- `authenticated: true` AND `pendingRegistration: true` → redirect to `/onboarding`
- `authenticated: true` AND `pendingRegistration: false` → allow access to `/dashboard`

---

### 1.7 Get Authenticated User Profile (Auth Context)

| | |
|---|---|
| **Endpoint** | `GET /auth/me` |
| **Auth** | `coagent_session` cookie (required) |
| **Purpose** | Returns Slack profile metadata from JWT claims + installation status |

**Response (200):**
```json
{
  "username": "john-agent",
  "pendingRegistration": false,
  "slack_name": "John Doe",
  "slack_workspace": "Acme Corp",
  "slack_workspace_domain": "acmecorp",
  "slack_email": "john@acmecorp.com",
  "slack_avatar_url": "https://avatars.slack-edge.com/...",
  "isSlackAppInstalled": true
}
```

**Error (401):** Unauthorized

---

### 1.8 Logout

| | |
|---|---|
| **Endpoint** | `POST /auth/logout` |
| **Auth** | `coagent_session` cookie |
| **Purpose** | Invalidates JWT (blacklists `jti`) and clears session cookie |

**Response (200):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```
*Clears `coagent_session` cookie (maxAge=0)*

---

## 2. User Profile Endpoints

**Controller:** `UserController` — no prefix (root level)

### 2.1 Get User Profile (Database)

| | |
|---|---|
| **Endpoint** | `GET /me` |
| **Auth** | `coagent_session` cookie (required) |
| **Purpose** | Returns user profile from database with connection status |

**Response (200 — Registered User):**
```json
{
  "username": "john-agent",
  "email": "john@acmecorp.com",
  "googleCalendarConnected": true,
  "createdAt": "2026-03-10T10:30:00Z"
}
```

**Response (200 — Pending User):**
```json
{
  "username": "",
  "pendingRegistration": true
}
```

**Error (401):**
```json
{ "error": "Authentication required" }
```

> **Note:** `GET /auth/me` returns Slack metadata from JWT claims. `GET /me` returns persisted user data from the database. The frontend should use `/auth/me` for profile display and `/me` for Google Calendar connection status.

---

## 3. Google Calendar Integration Endpoints

**Controller:** `IntegrationController` — prefix `/integrations/google`

### 3.1 Google OAuth Authorize

| | |
|---|---|
| **Endpoint** | `GET /integrations/google/authorize` |
| **Auth** | `coagent_session` cookie (required) |
| **Purpose** | Redirects to Google OAuth consent screen |

**Scopes Requested:** `calendar.events`, `calendar.readonly`  
**Access Type:** `offline` (for refresh token)  
**Prompt:** `consent`

**State Parameter:** Signed JWT containing `userId + nonce + timestamp` (CSRF protection)

**Response:**
- `302 Found` → Redirects to `https://accounts.google.com/o/oauth2/v2/auth?...`
- `401 Unauthorized` if not authenticated

---

### 3.2 Google OAuth Callback

| | |
|---|---|
| **Endpoint** | `GET /integrations/google/callback` |
| **Auth** | Validated via signed state token (not cookie-based) |
| **Purpose** | Exchanges code for tokens and stores encrypted credentials |

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `code` | string | No | Authorization code from Google |
| `error` | string | No | Error from Google |
| `state` | string | No | Signed JWT state token |

**Behavior:**
1. Validates signed state token (extracts userId)
2. Exchanges code for OAuth tokens via `OAuthTokenExchangePort`
3. Stores encrypted tokens via `ConnectServiceUseCase`

**Redirects:**
- Success → `{FRONTEND_URL}/onboarding?google=success`
- Error → `{FRONTEND_URL}/onboarding?google=error&reason={reason}`

---

### 3.3 Google Calendar Disconnect

| | |
|---|---|
| **Endpoint** | `DELETE /integrations/google/disconnect` |
| **Auth** | `coagent_session` cookie (required) |
| **Purpose** | Revokes Google Calendar connection |

**Response (200):**
```json
{
  "success": true,
  "message": "Google Calendar disconnected"
}
```

**Error (401):**
```json
{ "error": "Authentication required" }
```

**Error (500):**
```json
{ "error": "Failed to disconnect: {message}" }
```

---

### 3.4 Google Calendar Status

| | |
|---|---|
| **Endpoint** | `GET /integrations/google/status` |
| **Auth** | `coagent_session` cookie (required) |
| **Purpose** | Returns Google Calendar connection status |

**Response (200 — Connected):**
```json
{
  "service": "GOOGLE_CALENDAR",
  "connected": true
}
```

**Response (200 — Not Connected):**
```json
{
  "service": "GOOGLE_CALENDAR",
  "connected": false
}
```

---

## 4. Admin / Testing Endpoints

**Controller:** `RestApiController` — prefix `/api`

> These endpoints are primarily for local development and testing. They will be deprecated or secured for production.

### 4.1 Health Check

| | |
|---|---|
| **Endpoint** | `GET /api/health` |
| **Auth** | None (public) |
| **Purpose** | Application health check |

**Response (200):**
```
OK
```

---

### 4.2 Register User (Manual)

| | |
|---|---|
| **Endpoint** | `POST /api/users` |
| **Auth** | None |
| **Purpose** | Manual user registration (testing) |

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "slackUserId": "string",
  "workspaceId": "string"
}
```

**Response (200):** `"User registered successfully"`  
**Error (400):** Validation error message

---

### 4.3 Get User by ID

| | |
|---|---|
| **Endpoint** | `GET /api/users/{userId}` |
| **Auth** | None |
| **Purpose** | Get user profile by UUID (testing) |

**Response (200):**
```json
{
  "userId": "uuid-string",
  "username": "john-agent",
  "email": "john@example.com",
  "deleted": false
}
```

**Error (404):** Not found

---

### 4.4 Provision Agent (Manual)

| | |
|---|---|
| **Endpoint** | `POST /api/agents` |
| **Auth** | None |
| **Purpose** | Provision an agent for a user (testing, idempotent) |

**Request Body:**
```json
{
  "userId": "uuid-string"
}
```

**Response (200):** `"Agent provisioned: {agentId}"`  
**Error (400):** User not found or invalid format

---

### 4.5 Google OAuth Authorize (Legacy)

| | |
|---|---|
| **Endpoint** | `GET /api/oauth2/authorize` |
| **Auth** | None (userId passed as query param) |
| **Purpose** | Legacy Google OAuth start (testing only) |

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | string | Yes | UUID of the user |

**Response:**
- `302 Found` → Redirects to Google OAuth

> **Deprecated:** Use `GET /integrations/google/authorize` instead.

---

### 4.6 Google OAuth Callback (Legacy)

| | |
|---|---|
| **Endpoint** | `GET /api/oauth2/callback` |
| **Auth** | None (userId from state param) |
| **Purpose** | Legacy Google OAuth callback (testing only) |

**Response (200):** `"Google Calendar connected successfully! You can close this window."`

> **Deprecated:** Use `GET /integrations/google/callback` instead.

---

### 4.7 Sandbox: Parse Intent

| | |
|---|---|
| **Endpoint** | `POST /api/sandbox/parse-intent` |
| **Auth** | None |
| **Purpose** | Test intent parsing (Tier 1 regex + Tier 2 LLM fallback) |

**Request Body:**
```json
{
  "text": "@CoAgent4U schedule meeting with @john Friday evening",
  "forceLlm": false
}
```

**Response (200):**
```json
{
  "tier1Type": "SCHEDULE_MEETING",
  "tier1Params": {
    "target_user": "john",
    "timeframe": "Friday evening"
  },
  "tier2Type": "SKIPPED",
  "finalDecision": "SCHEDULE_MEETING",
  "debugStatus": "Tier 1 matched"
}
```

---

## 5. Slack Webhook Endpoints

**Module:** `messaging-module` (integration layer)

### 5.1 Slack Events API

| | |
|---|---|
| **Endpoint** | `POST /slack/events` |
| **Auth** | HMAC-SHA256 signature verification (`X-Slack-Signature`) |
| **Purpose** | Receives all Slack Events API webhooks |

**Required Headers:**

| Header | Description |
|--------|-------------|
| `X-Slack-Request-Timestamp` | Unix timestamp from Slack |
| `X-Slack-Signature` | HMAC-SHA256 signature |

**Handled Event Types:**
- `url_verification` — Responds with challenge (Slack app setup)
- `event_callback` → `message` / `app_mention` — Routes to `HandleMessageUseCase`
- `event_callback` → `app_uninstalled` — Marks workspace installation as inactive

**Behavior:**
- Returns `200 OK` immediately (within 3s Slack deadline)
- Processes message asynchronously on dedicated thread pool
- Deduplicates by `event_id` (10-minute TTL cache)
- Strips bot self-mention, preserves `<@USER_ID>` as `slack:USER_ID`
- Skips: bot messages, `message_changed`, `message_deleted`

---

### 5.2 Slack Interactive Components

| | |
|---|---|
| **Endpoint** | `POST /slack/interactions` |
| **Auth** | HMAC-SHA256 signature verification (`X-Slack-Signature`) |
| **Content-Type** | `application/x-www-form-urlencoded` |
| **Purpose** | Handles Slack Block Kit button clicks (approvals, slot selection) |

**Handled Actions:**
- `slot_select_{coordId}_{index}` — User selects a proposed time slot
- `reject_coords_{coordId}` — Invitee rejects coordination (early rejection)
- `approve_action` — User approves a personal/collaborative proposal
- `reject_action` — User rejects a personal/collaborative proposal

**Behavior:**
- Returns `200 OK` immediately
- Processes on background thread
- Idempotency guard (prevents double-processing of rapid clicks, 500-entry cache)
- Deletes interactive card after action
- Final notifications handled by `CoordinationCompletedListener`

---

## 6. Cross-Cutting Concerns

### 6.1 Authentication (JWT Filter)

**Filter:** `JwtAuthenticationFilter`

- Reads JWT from `coagent_session` HTTP-only cookie
- Validates via `JwtValidator.validateFull()`
- Checks blacklist via `JwtTokenBlacklist.isRevoked()`
- Sets `AuthenticatedUser` record in request attribute

**Public Endpoints (skip auth):**
- `/auth/slack/*` — OAuth flows
- `/api/health` — Health check
- `/actuator/*` — Spring Actuator
- `/slack/*` — Slack webhooks (use HMAC-SHA256 instead)

### 6.2 Rate Limiting

**Filter:** `RateLimitFilter`

- Uses Caffeine-based rate limiter
- Applies to all authenticated endpoints
- Returns `429 Too Many Requests` when limit exceeded

### 6.3 Cookie Configuration

| Property | Value |
|----------|-------|
| Name | `coagent_session` |
| HttpOnly | `true` |
| Secure | `true` |
| SameSite | `None` |
| Domain | `.coagent4u.com` |
| Path | `/` |
| Max-Age | `86400` (24 hours) |

### 6.4 CORS

- Allowed origin: `{FRONTEND_URL}` (from config)
- Credentials: `true` (for HTTP-only cookies)

---

## 7. Error Handling

All endpoints follow a consistent error response format:

```json
{
  "error": "Human-readable error message"
}
```

**HTTP Status Codes:**

| Code | Meaning |
|------|---------|
| `200` | Success |
| `302` | Redirect (OAuth flows) |
| `400` | Bad request (invalid input) |
| `401` | Unauthorized (missing or invalid session) |
| `404` | Resource not found |
| `409` | Conflict (e.g. duplicate resource) |
| `429` | Rate limit exceeded |
| `500` | Internal server error |

---

## Endpoint Summary Table

| Method | Path | Auth | Controller | Purpose |
|--------|------|------|------------|---------|
| `GET` | `/auth/slack/start` | Public | AuthController | Start Slack OAuth login |
| `GET` | `/auth/slack/callback` | Public | AuthController | Handle Slack OAuth callback |
| `GET` | `/auth/slack/install/start` | Public | AuthController | Start Slack App installation |
| `GET` | `/auth/slack/install/callback` | Public | AuthController | Handle Slack App install callback |
| `POST` | `/auth/username` | Cookie (pending) | AuthController | Complete onboarding |
| `GET` | `/auth/session` | Cookie (optional) | AuthController | Check session status |
| `GET` | `/auth/me` | Cookie | AuthController | Get Slack profile from JWT |
| `POST` | `/auth/logout` | Cookie | AuthController | Logout and revoke JWT |
| `GET` | `/me` | Cookie | UserController | Get user profile from DB |
| `GET` | `/integrations/google/authorize` | Cookie | IntegrationController | Start Google OAuth |
| `GET` | `/integrations/google/callback` | State token | IntegrationController | Handle Google OAuth callback |
| `DELETE` | `/integrations/google/disconnect` | Cookie | IntegrationController | Disconnect Google Calendar |
| `GET` | `/integrations/google/status` | Cookie | IntegrationController | Check Google Calendar status |
| `GET` | `/api/health` | Public | RestApiController | Health check |
| `POST` | `/api/users` | None | RestApiController | Register user (testing) |
| `GET` | `/api/users/{userId}` | None | RestApiController | Get user by ID (testing) |
| `POST` | `/api/agents` | None | RestApiController | Provision agent (testing) |
| `GET` | `/api/oauth2/authorize` | None | RestApiController | Legacy Google OAuth (deprecated) |
| `GET` | `/api/oauth2/callback` | None | RestApiController | Legacy Google callback (deprecated) |
| `POST` | `/api/sandbox/parse-intent` | None | RestApiController | Test intent parsing |
| `POST` | `/slack/events` | HMAC-SHA256 | SlackInboundAdapter | Slack Events API webhook |
| `POST` | `/slack/interactions` | HMAC-SHA256 | SlackInteractionHandler | Slack interactive components |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SLACK_CLIENT_ID` | Slack OAuth App client ID | — |
| `SLACK_CLIENT_SECRET` | Slack OAuth App client secret | — |
| `SLACK_REDIRECT_URI` | Slack OAuth redirect URI | `http://localhost:8080/auth/slack/callback` |
| `SLACK_SIGNING_SECRET` | Slack app signing secret (for HMAC) | — |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | — |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | — |
| `GOOGLE_REDIRECT_URI` | Google OAuth redirect URI | — |
| `FRONTEND_URL` | Frontend URL for CORS and redirects | `http://localhost:3000` |
| `JWT_SECRET` | Secret key for JWT signing | — |
| `AES_SECRET_KEY` | AES-256 key for token encryption | — |
