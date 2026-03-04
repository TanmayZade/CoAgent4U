# Phase 3 — Integration Adapters

## Goal
Connect domain core to Slack, Google Calendar, and Groq LLM. Expose REST API.

## Cross-Cutting Concerns (All Adapters)

### Error Mapping Layer
Each adapter maps external errors to domain exceptions — **no HTTP codes leak upward**.

| External Error | Domain Exception |
|---|---|
| Google 401 | `TokenExpiredException` |
| Google 429 / 5xx | `ExternalServiceUnavailableException` |
| Slack 403 | `NotificationFailureException` |
| Groq timeout / 5xx | `LLMUnavailableException` |

> [!IMPORTANT]
> All exceptions defined in a new `com.coagent4u.common.exception` package in `common-domain`.

### Resilience (Lightweight — Phase 5 hardens further)
- **WebClient timeouts**: connect 3s, read 5s (from [CoagentProperties](file:///e:/CoAgent4U/infrastructure/config/src/main/java/com/coagent4u/config/CoagentProperties.java#9-229))
- **Retry**: max 2 attempts, exponential backoff (500ms base)
- **Correlation ID**: `X-Correlation-Id` MDC propagation on all outbound calls

### Async Slack Webhook Processing

> [!CAUTION]
> Slack requires HTTP 200 within **3 seconds** or it retries.

- `SlackInboundAdapter` immediately returns `200 OK`
- Delegates to [AgentCommandService](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#42-144) via `@Async` on a bounded thread pool
- Idempotency guard: deduplicate by Slack `event_id`

---

## Proposed Changes

### Step 3.1 — Messaging Module (`integration/messaging-module`)

#### [NEW] `SlackInboundAdapter.java`
- `@RestController` at `/slack/events`
- Signature verification via `SlackSignatureVerifier`
- URL verification challenge handler
- User resolution: `SlackUserId` → `AgentId` via `UserQueryPort`
- **Async delegation** to `HandleMessageUseCase`
- Idempotency via in-memory `ConcurrentHashMap<String, Instant>` (event_id dedup)

#### [NEW] `SlackNotificationAdapter.java`
- Implements `NotificationPort`
- Uses `WebClient` to call Slack `chat.postMessage` API
- Block Kit message builder for approval prompts
- Error mapping: Slack errors → `NotificationFailureException`

#### [MODIFY] `pom.xml`
- Add `spring-boot-starter-web`, `spring-boot-starter-webflux` (WebClient), `coagent4u-security`, `coagent4u-config`

---

### Step 3.2 — Calendar Module (`integration/calendar-module`)

#### [NEW] `GoogleCalendarAdapter.java`
- Implements `CalendarPort`
- `WebClient` calls to Google Calendar API v3
- FreeBusy queries, event CRUD
- OAuth token refresh using encrypted refresh tokens (`AesEncryptionService`)
- Error mapping: Google API errors → domain exceptions

#### [MODIFY] `pom.xml`
- Add `spring-boot-starter-webflux`, `coagent4u-security`, `coagent4u-config`, `jackson-databind`

---

### Step 3.3 — LLM Module (`integration/llm-module`)

#### [NEW] `GroqLLMAdapter.java`
- Implements `LLMPort`
- `WebClient` POST to `https://api.groq.com/openai/v1/chat/completions`
- Structured prompt for intent classification
- Schedule summarization prompt
- Error → `Optional.empty()` (Tier 1 rule-based fallback)

#### [MODIFY] `pom.xml`
- Add `spring-boot-starter-webflux`, `coagent4u-config`, `jackson-databind`

---

### Step 3.4 — Application Layer (`coagent-app`)

#### [NEW] `RestApiController.java`
- `POST /api/users` — registration
- `GET /api/users/{id}/schedule` — view schedule
- `POST /api/users/{id}/approvals/{approvalId}/decide` — approve/reject
- `GET /api/users/{id}/connections` — service connections
- `GET /oauth2/callback` — Google OAuth callback

#### [NEW] `WebClientConfig.java`
- Shared `WebClient.Builder` bean with timeouts + retry filter + correlation ID

---

### Step 3.5 — Common Domain Exceptions

#### [NEW] `common-domain/.../exception/` package
- `ExternalServiceUnavailableException`
- `TokenExpiredException`
- `NotificationFailureException`
- `LLMUnavailableException`

---

## Verification Plan

### Adapter Integration Tests (WireMock)

| Test Class | Verifies |
|---|---|
| `SlackInboundAdapterIT` | Signature check, async delegation, URL challenge, idempotency |
| `SlackNotificationAdapterIT` | Block Kit payload shape, error mapping |
| `GoogleCalendarAdapterIT` | FreeBusy, create event, token refresh, error mapping |
| `GroqLLMAdapterIT` | Intent parse, UNKNOWN fallback, timeout handling |

### Controller Tests (MockMvc)
| Test Class | Verifies |
|---|---|
| `RestApiControllerTest` | Endpoints, OAuth callback, approval decision |

### Full Context Boot
- `@SpringBootTest` — all `Port` → `Adapter` beans resolved, no missing beans

### Exit Criteria (Strict)
- ✔ Slack message creates domain flow (async)
- ✔ Approval prompt sent back to Slack
- ✔ Google FreeBusy query works
- ✔ Google event CRUD works
- ✔ Groq fallback triggers on error
- ✔ Full Spring context boots with all adapters wired
- ✔ No architecture violations
