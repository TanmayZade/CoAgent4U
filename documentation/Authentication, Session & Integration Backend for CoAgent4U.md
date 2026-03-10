# Authentication, Session & Integration Backend for CoAgent4U

Implement Slack OAuth login, JWT session management, Google Calendar integration, and supporting infrastructure so the frontend can authenticate users and manage integrations.

## User Review Required

> [!IMPORTANT]
> **Slack OAuth requires a Slack App with OAuth configured.** The Slack App must have `users:read` and `users:read.email` OAuth scopes enabled, and `openid`, `profile`, `email` scopes for Sign-in with Slack. The OAuth redirect URI in your Slack App settings must point to `http://localhost:8080/auth/slack/callback` (for local dev).

> [!WARNING]
> **JWT Token Revocation Strategy**: Using an in-memory Caffeine-based blacklist for MVP. This works for single-instance deployments. For multi-instance, a shared Redis store would be needed (out of scope for MVP).

> [!IMPORTANT]
> **New env vars required.** The following must be set:
> - `SLACK_CLIENT_ID` — Slack OAuth App client ID
> - `SLACK_CLIENT_SECRET` — Slack OAuth App client secret  
> - `SLACK_REDIRECT_URI` — defaults to `http://localhost:8080/auth/slack/callback`
> - `FRONTEND_URL` — defaults to `http://localhost:3000` (for CORS)

---

## Proposed Changes

### Infrastructure / Security Module

Infrastructure primitives (no Spring dependencies, pure Java). Used by the config module to wire beans.

#### [MODIFY] [JwtIssuer.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtIssuer.java)
- Add [issue(UUID userId, String username)](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtIssuer.java#40-50) overload that embeds `username` as a JWT claim
- Keep existing [issue(UUID userId)](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtIssuer.java#40-50) for backward compatibility

#### [MODIFY] [JwtValidator.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtValidator.java)
- Add `JwtClaims` record (inner class) with `userId`, `username`, `issuedAt`, `expiry`
- Add `validateFull(String token)` that returns `Optional<JwtClaims>`
- Keep existing [validate(String token)](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtValidator.java#25-41) for backward compatibility

#### [NEW] [JwtTokenBlacklist.java](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/JwtTokenBlacklist.java)
- Caffeine-based in-memory blacklist keyed by JWT `jti` claim
- Entries expire after JWT max TTL (24h) — no memory leak
- Methods: [revoke(String jti)](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/ServiceConnection.java#34-38), `isRevoked(String jti)`

---

### Infrastructure / Config Module

Spring bean wiring for the security components.

#### [MODIFY] [CoagentProperties.java](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java)
- Add `clientId`, `clientSecret`, `redirectUri` to the existing [Slack](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java#67-96) inner class
- Add `frontendUrl` field at top level

#### [MODIFY] [SecurityBeanConfig.java](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/SecurityBeanConfig.java)
- Add `@Bean JwtTokenBlacklist` wired from CoagentProperties

#### [MODIFY] [application.yml](file:///e:/CoAgent4U/infrastructure/config/src/main/resources/application.yml)
- Add Slack OAuth config under `coagent.slack`: `client-id`, `client-secret`, `redirect-uri`
- Add `coagent.frontend-url`
- Fix Google `redirect-uri` to use new `/integrations/google/callback` path

---

### Core / User Module

Add a new outbound port for Slack OAuth identity resolution.

#### [NEW] [SlackOAuthPort.java](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/SlackOAuthPort.java)
- Outbound port: [exchangeCode(String code) → SlackOAuthResult](file:///e:/CoAgent4U/integration/calendar-module/src/main/java/com/coagent4u/calendar/GoogleCalendarAdapter.java#87-144)
- `SlackOAuthResult` record: `slackUserId`, `workspaceId`, `email`, `displayName`, `accessToken`

---

### Integration / Messaging Module

Adapter that calls Slack's OAuth API.

#### [NEW] [SlackOAuthAdapter.java](file:///e:/CoAgent4U/integration/messaging-module/src/main/java/com/coagent4u/messaging/SlackOAuthAdapter.java)
- Implements `SlackOAuthPort`
- Exchanges code via `https://slack.com/api/oauth.v2.access`
- Retrieves user identity via `https://slack.com/api/users.identity` or from the `authed_user` payload
- Returns `SlackOAuthResult` with user details

#### [MODIFY] [pom.xml](file:///e:/CoAgent4U/integration/messaging-module/pom.xml)
- Add dependency on `user-module` (for `SlackOAuthPort` interface)

---

### Application Assembly (coagent-app)

New controllers, filter, and wiring.

#### [NEW] [AuthController.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/rest/AuthController.java)
Endpoints:
- `GET /auth/slack/start` — Redirects to Slack OAuth consent screen
- `GET /auth/slack/callback` — Handles Slack OAuth callback:
  1. Exchanges code for Slack tokens
  2. Resolves Slack user identity
  3. Looks up user by `slackUserId`
  4. **Existing user**: Issues JWT in HTTPOnly cookie, redirects to frontend
  5. **New user**: Creates temporary session (JWT with `pending_registration=true`), redirects to frontend onboarding page
- `POST /auth/username` — For new users:
  1. Validates username format
  2. Creates User via [RegisterUserUseCase](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/in/RegisterUserUseCase.java#11-25)
  3. Provisions Agent via [AgentPersistencePort](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/port/out/AgentPersistencePort.java#13-20) (idempotent)
  4. Reissues JWT with full claims (removes `pending_registration`)
  5. Returns success with updated cookie
- `POST /auth/logout` — Blacklists JWT `jti`, clears cookie

#### [NEW] [IntegrationController.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/rest/IntegrationController.java)
Endpoints (all require JWT auth):
- `GET /integrations/google/authorize` — Redirects to Google OAuth with `state=userId`
- `GET /integrations/google/callback` — Exchanges code, stores encrypted tokens
- `DELETE /integrations/google/disconnect` — Revokes Google Calendar connection
- `GET /integrations/google/status` — Returns connection status

#### [NEW] [UserController.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/rest/UserController.java)
- `GET /me` — Returns authenticated user profile (from JWT claims + DB lookup)

#### [NEW] [JwtAuthenticationFilter.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/filter/JwtAuthenticationFilter.java)
- Extends `OncePerRequestFilter`
- Reads JWT from `coagent_session` HTTPOnly cookie
- Validates via `JwtValidator.validateFull()`
- Checks blacklist via `JwtTokenBlacklist.isRevoked()`
- Sets `AuthenticatedUser` record in request attribute
- Skips public endpoints: `/auth/slack/*`, `/api/health`, `/actuator/*`, Slack event endpoints

#### [NEW] [RateLimitFilter.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/filter/RateLimitFilter.java)
- Uses existing [CaffeineRateLimiter](file:///e:/CoAgent4U/infrastructure/security/src/main/java/com/coagent4u/security/CaffeineRateLimiter.java#13-44)
- Applies to all authenticated endpoints
- Returns HTTP 429 when limit exceeded

#### [NEW] [AuthenticatedUser.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/security/AuthenticatedUser.java)
- Record: `userId`, `username`, `pendingRegistration`
- Stored in request attribute `AUTHENTICATED_USER`
- Utility method `AuthenticatedUser.from(HttpServletRequest)` for controllers

#### [NEW] [CorsConfig.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/config/CorsConfig.java)
- Configures CORS for frontend origin
- Allows credentials (for HTTPOnly cookies)
- Allowed origins from `CoagentProperties.frontendUrl`

#### [NEW] [FilterRegistrationConfig.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/config/FilterRegistrationConfig.java)
- Registers `JwtAuthenticationFilter` and `RateLimitFilter` as beans with proper ordering

#### [MODIFY] [BeanWiringConfig.java](file:///e:/CoAgent4U/coagent-app/src/main/java/com/coagent4u/app/config/BeanWiringConfig.java)
- Add bean wiring for `SlackOAuthAdapter` → `SlackOAuthPort`

#### [MODIFY] [application.properties](file:///e:/CoAgent4U/coagent-app/src/main/resources/application.properties)
- Add Slack OAuth env vars: `SLACK_CLIENT_ID`, `SLACK_CLIENT_SECRET`, `SLACK_REDIRECT_URI`
- Add `FRONTEND_URL`

---

### Infrastructure / Persistence Module

#### [NEW] [V10__auth_session_tables.sql](file:///e:/CoAgent4U/infrastructure/persistence/src/main/resources/db/migration/V10__auth_session_tables.sql)
```sql
-- Token blacklist for JWT revocation (logout support)
CREATE TABLE revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    user_id     UUID NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_revoked_tokens_expires ON revoked_tokens(expires_at);

-- Add google_account_id to service_connections
ALTER TABLE service_connections 
    ADD COLUMN IF NOT EXISTS google_account_id VARCHAR(128);

-- Add disconnected_at column
ALTER TABLE service_connections
    ADD COLUMN IF NOT EXISTS disconnected_at TIMESTAMPTZ;
```

> [!NOTE]  
> The `revoked_tokens` table is for persistence across restarts. The in-memory Caffeine cache is the primary lookup for performance. On app boot, expired rows can be pruned. For MVP, the in-memory blacklist is sufficient since JWTs have a 24h TTL.

---

## Verification Plan

### Automated Tests

The project uses **Maven + JUnit 5**. Existing tests are standalone MockMvc or plain unit tests (no Spring context boot needed for most).

**Run all existing tests** to verify no regressions:
```bash
cd e:\CoAgent4U
mvn test -pl infrastructure/security -pl coagent-app
```

**Existing tests that will validate our changes:**
- [SecurityTests.java](file:///e:/CoAgent4U/infrastructure/security/src/test/java/com/coagent4u/security/SecurityTests.java) — JWT round-trip, AES encrypt/decrypt, rate limiter, Slack signature verification
- [RestApiControllerTest.java](file:///e:/CoAgent4U/coagent-app/src/test/java/com/coagent4u/app/rest/RestApiControllerTest.java) — existing REST endpoint tests (may need updates if controller changes)

### Manual Verification

> [!TIP]
> After implementation, I will ask you to run `mvn clean install -DskipTests` to verify the full project compiles, and then `mvn test` to run all tests. Please share the output with me.

**Manual API Testing (after app is running):**

1. **Health check**: `GET http://localhost:8080/api/health` → should return `200 OK`
2. **Slack login start**: `GET http://localhost:8080/auth/slack/start` → should redirect (302) to Slack OAuth URL
3. **Logout**: `POST http://localhost:8080/auth/logout` with session cookie → should clear cookie
4. **Rate limiting**: Send 101 rapid requests → 101st should return `429`
5. **Google authorize**: `GET http://localhost:8080/integrations/google/authorize` with session cookie → should redirect to Google OAuth
6. **GET /me**: With valid session cookie → should return user profile JSON
