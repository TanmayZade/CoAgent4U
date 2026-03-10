# Phase 5 Manual Testing Guide

## 1. Prerequisites

App must be running:
```bash
mvn clean install -DskipTests

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

for authorization of tanmay's google calendar
http://localhost:8080/api/oauth2/authorize?userId=4be3a87d-b119-44b4-bddb-1e41f4e7fd2c

for authorization of pranav's google calendar
http://localhost:8080/api/oauth2/authorize?userId=38170dc0-6b9a-4e00-81dd-331325790255

for authorization of rohit's google calendar
http://localhost:8080/api/oauth2/authorize?userId=518cd6ae-ed7b-4422-9cc8-89c662a015a6





You are implementing backend capabilities for CoAgent4U, a deterministic personal agent coordination platform built with:
Java 21
Spring Boot 3.4
Modular Monolith
Hexagonal Architecture (Ports & Adapters)
PostgreSQL
Flyway migrations
JWT authentication
Slack integration
Google Calendar OAuth
The system architecture is strictly layered:
Domain
Application
Ports
Adapters (Inbound / Outbound)
Infrastructure
Business logic must never exist in adapters.
The system must remain deterministic, secure, and GDPR aligned.

TASK
Prepare the backend for the authentication, session, and integration capabilities required for the MVP.
The backend must support the following operations.

1. Slack Based Sign In
Implement Slack OAuth 2.0 login.
Flow:
User clicks "Sign in with Slack"
Backend redirects user to Slack OAuth authorization
Slack returns authorization code
Backend exchanges the code for access token
Backend retrieves Slack user identity
Backend checks if the user exists
First Time User Flow
If user does not exist:
Create a temporary user session
Ask the user to choose a username
After username submission:
Create the User record
Provision the Personal AI Agent
Persist the agent in the agent-module
Mark onboarding complete

Agent provisioning must be idempotent.

2. Logout Functionality
Provide logout capability.
Requirements:
Invalidate JWT
Remove session cookie
Clear authentication context
Expose endpoint:
POST /auth/logout
JWT must not remain usable after logout.
Use token revocation or short expiration strategy.

3. JWT Based Session Management
After successful login:
Issue JWT token
Expiration: 24 hours
JWT must contain:
user_id
username
issued_at
expiry
JWT must be stored in:
HTTPOnly secure cookie.
Use:
AES-256 signing key.
Middleware must validate JWT on every request.

4. Google Calendar Authorization
Users may optionally connect Google Calendar.
Implement OAuth2 integration.
Capabilities:
Authorize Google Calendar
Revoke Google Calendar access
Endpoints:
GET /integrations/google/authorize
GET /integrations/google/callback
DELETE /integrations/google/disconnect
When connected:
Store encrypted OAuth tokens (AES-256)
Persist refresh token
Database fields:
google_account_id
encrypted_access_token
encrypted_refresh_token
token_expiry
Tokens must be encrypted before storing.

5. Google Calendar Disconnection
If user disconnects Google Calendar:
Remove tokens
Mark integration as disabled
Coordination module must stop using calendar adapter
User must be able to reconnect later.

6. Module Placement
Implementation must follow modular boundaries.
infrastructure/security module
Handles:
Slack login
JWT authentication and validation
login
signup
logout
session management
user-module
Handles:
User entity
username selection
onboarding completion
agent-module
Handles:
agent provisioning
agent lifecycle
integration-module
Handles:
Google OAuth
token storage
integration status
Adapters must only translate external protocols.

7. Security Requirements
Implement:
Slack signature verification
JWT expiration validation
HTTPS-only cookies
AES-256 token encryption
Rate limiting (100 req/min per user)
Never store plaintext OAuth tokens.

8. API Endpoints Required
Auth:
GET /auth/slack/start
GET /auth/slack/callback
POST /auth/username
POST /auth/logout
Integration:
GET /integrations/google/authorize
GET /integrations/google/callback
DELETE /integrations/google/disconnect
User:
GET /me

10. Deliverables
Generate:
Domain entities
Application services
Ports
Adapters
Controllers
JWT security filter
OAuth clients
Flyway migrations
Code must follow clean architecture and modular monolith practices.
Avoid introducing unnecessary frameworks.
All logic must remain deterministic and auditable.


