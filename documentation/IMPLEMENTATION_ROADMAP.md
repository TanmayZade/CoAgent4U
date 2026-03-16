# CoAgent4U — Implementation Roadmap

> **Purpose:** This document is the single source of truth for the MVP implementation plan. Every decision, file path, and build order is traced back to the PRD, arc42, and C4 documentation. No code is generated here — only the roadmap that guides it.
>
> **Audience:** Solo developer implementing the MVP.

---

## Table of Contents

- [1. Development Phases](#1-development-phases)
- [2. Folder Structure](#2-folder-structure)
- [3. Module Implementation Order](#3-module-implementation-order)
- [4. Dependency Strategy](#4-dependency-strategy)
- [5. Interface-First Development](#5-interface-first-development)
- [6. Database Evolution](#6-database-evolution)
- [7. Testing Strategy](#7-testing-strategy)
- [8. CI/CD Approach](#8-cicd-approach)
- [9. Risk Mitigation](#9-risk-mitigation)
- [10. MVP Completion Definition](#10-mvp-completion-definition)

---

## 1. Development Phases

### Phase 0 — Project Bootstrap (Days 1–3)

**Goal:** Prove the build system works end-to-end with zero business logic.

| Step | Action | Validation |
|------|--------|------------|
| 0.1 | Create root Maven POM with `<modules>` for all planned Maven modules. Set Java 21, Spring Boot 3.x BOM, Maven enforcer plugin. | `mvn clean compile` succeeds with zero modules containing code. |
| 0.2 | Create `shared-kernel` module — empty package with placeholder `UserId` value object (record). | Compiles. Zero Spring dependencies in this module's POM. |
| 0.3 | Create `common-domain` module — `DomainEvent` marker interface and `DomainEventPublisher` port interface. Depends only on `shared-kernel`. | Compiles. Zero Spring/JPA imports. |
| 0.4 | Create skeleton modules for all core, integration, and infrastructure modules with empty `package-info.java`. | Full `mvn clean compile` succeeds across all modules. |
| 0.5 | Create `docker-compose.yml` with PostgreSQL 15 and app placeholder. Validate DB starts with `docker-compose up coagent-db`. | `pg_isready` returns success. |
| 0.6 | Create `coagent-app` Spring Boot application module that assembles all modules. Add `application-dev.yml`. Validate Spring context boots with `mvn spring-boot:run`. | Health endpoint at `/actuator/health` returns `UP`. |
| 0.7 | Create initial Flyway migration `V1__init_schema.sql` with all tables from PRD §8.6 schema. Validate migrations run on startup. | All tables exist in PostgreSQL. |

**Exit Criterion:** `mvn clean package` produces a runnable JAR. Docker Compose starts PostgreSQL + app. Flyway creates schema. Health check returns `UP`.

---

### Phase 1 — Domain Core (Days 4–12)

**Goal:** Implement pure domain logic and port interfaces — zero infrastructure code.

| Step | Module | Action | Artifacts | Tests |
|------|--------|--------|-----------|-------|
| 1.1 | `shared-kernel` | Define all shared value objects: `UserId`, `AgentId`, `CoordinationId`, `ApprovalId`, `TimeSlot`, `TimeRange`, `Email`, `Duration`, `EventId`, `SlackUserId`, `WorkspaceId`. | ~12 record classes. | Constructor validation tests. |
| 1.2 | `common-domain` | Define `DomainEvent` base, `DomainEventPublisher` port. Define published event types: `UserRegistered`, `UserDeleted`, `AgentProvisioned`, `PersonalEventCreated`, `CoordinationStateChanged`, `CoordinationCompleted`, `CoordinationFailed`, `ApprovalDecisionMade`, `ApprovalExpired`. | ~11 event classes + port interface. | Instantiation and serialization tests. |
| 1.3 | `user-module` | Domain: `User` aggregate, `SlackIdentity` entity, `ServiceConnection` entity, `UserConnectionStatus` VO. Ports: `UserPersistencePort`, `UserQueryPort`, `NotificationPort` (outbound). Inbound: `RegisterUserUseCase`, `ConnectServiceUseCase`, `DisconnectServiceUseCase`, `DeleteUserUseCase`. Application: `UserManagementService`. | ~10 domain + port + service classes. | Domain aggregate lifecycle tests, service tests with mocked ports. |
| 1.4 | `approval-module` | Domain: `Approval` aggregate, `ApprovalStatus` VO, `ApprovalType` VO, `ExpirationPolicy` domain service. Ports: `ApprovalPersistencePort`, `NotificationPort` (outbound), `DomainEventPublisher`. Inbound: `CreateApprovalUseCase`, `DecideApprovalUseCase`. Application: `ApprovalService`. | ~8 domain + port + service classes. | Approval lifecycle tests: create → approve → event published. Expiration tests. |
| 1.5 | `coordination-module` | Domain: `Coordination` aggregate, `CoordinationStateMachine` domain service, `AvailabilityMatcher` domain service, `ProposalGenerator` domain service, `EventCreationSaga`. VOs: `AvailabilityBlock`, `MeetingProposal`, `CoordinationState` (enum), `EventConfirmation`, `AgentProfile`, `ProtocolMessage`, `TerminationReason`, `SchedulingRequest`, `SchedulingConstraints`. Ports: `CoordinationPersistencePort`, `AgentAvailabilityPort`, `AgentEventExecutionPort`, `AgentProfilePort`, `AgentApprovalPort`, `DomainEventPublisher`. Inbound: `CoordinationProtocolPort`. Application: `CoordinationService` (implements `CoordinationProtocolPort`). | ~25 domain + port + service classes. | **Exhaustive** state machine transition tests (all 14 states). Matcher/Generator unit tests. Saga happy-path and compensation tests (mocked agent ports). |
| 1.6 | `agent-module` | Domain: `Agent` aggregate, `IntentParser` domain service (rule-based Tier 1), `ConflictDetector` domain service, `ParsedIntent` VO, `IntentType` enum. Ports: `CalendarPort`, `LLMPort`, `ApprovalPort`, `AgentPersistencePort`, `UserQueryPort`, `NotificationPort`, `CoordinationProtocolPort` (outbound from agent), `DomainEventPublisher`. Agent capability port implementations: `AgentAvailabilityPortImpl`, `AgentEventExecutionPortImpl`, `AgentProfilePortImpl`, `AgentApprovalPortImpl`. Inbound: `HandleMessageUseCase`, `ViewScheduleUseCase`, `CreatePersonalEventUseCase`. Application: `AgentCommandService`. Event handlers: `CollaborativeApprovalDecisionHandler`, `CollaborativeApprovalExpiredHandler`, `PersonalApprovalDecisionHandler`, `PersonalApprovalExpiredHandler`. | ~30 domain + port + service + handler classes. | IntentParser pattern matching tests. ConflictDetector tests. Capability port impl tests with mocked CalendarPort/UserQueryPort. Handler routing tests. |

**Exit Criterion:** All four core modules compile cleanly. Zero Spring/JPA/Slack/Google imports in any domain package. All domain and application layer tests pass. `common-domain` and `shared-kernel` have zero external dependencies.

---

### Phase 2 — Persistence & Infrastructure (Days 13–20)

**Goal:** Wire domain logic to PostgreSQL and cross-cutting infrastructure.

| Step | Module | Action | Tests |
|------|--------|--------|-------|
| 2.1 | `persistence` | Create JPA entity classes: `UserJpaEntity`, `AgentJpaEntity`, `CoordinationJpaEntity`, `ApprovalJpaEntity`, `AgentActivityJpaEntity`, etc. Create mapper classes. Implement all five `PersistencePort` adapters with Spring Data repositories. | Repository integration tests with Testcontainers (PostgreSQL). CRUD + query tests for each adapter. |
| 2.2 | `persistence` | Refine Flyway migrations: indexes, constraints, JSONB columns for proposals. Verify schema matches JPA entities. | `ddl-auto=validate` passes on startup. |
| 2.3 | `security` | Implement `JwtValidator` + `JwtIssuer` (HS256). Implement `AesTokenEncryption` (AES-256-GCM). Implement `SlackSignatureVerifier` (HMAC-SHA256). Implement `CaffeineRateLimiter`. | Unit tests for each component. JWT round-trip test. AES encrypt/decrypt round-trip test. Signature verification test. |
| 2.4 | `config` | Create `application.yml`, `application-dev.yml`, `application-staging.yml` profiles. Define all `@ConfigurationProperties` classes. | Config loads on Spring context boot. |
| 2.5 | `monitoring` | Implement `SpringEventPublisherAdapter` (implements `DomainEventPublisher`). Create async event handlers: `AgentActivityEventHandler`, `MetricsEventHandler`, `StructuredLogHandler`. Configure Micrometer with custom metrics. | Event publishing and handler integration test. Verify event loss doesn't affect coordination state. |

**Exit Criterion:** Full Spring context boots. Domain services wired to PostgreSQL via persistence adapters. Flyway migrations run. JWT, AES, Slack signature verification work. Event bus dispatches events.

---

### Phase 3 — Integration Adapters (Days 21–30)

**Goal:** Connect to Slack, Google Calendar, and Groq LLM.

| Step | Module | Action | Tests |
|------|--------|--------|-------|
| 3.1 | `messaging-module` | Implement `SlackInboundAdapter` (primary/driving): webhook endpoint at `/slack/events`, signature verification, payload parsing, user resolution, intent routing. Implement `SlackNotificationAdapter` (secondary/driven): `NotificationPort` implementation with Block Kit message formatting. | WireMock-based integration tests. Signature verification test. Block Kit payload structure tests. |
| 3.2 | `calendar-module` | Implement `GoogleCalendarAdapter` (secondary/driven): `CalendarPort` implementation with OAuth token lifecycle, FreeBusy queries, event CRUD, token refresh. Error mapping for Google API errors. | WireMock-based integration tests. Token refresh flow test. Error mapping tests. |
| 3.3 | `llm-module` | Implement `GroqLLMAdapter` (secondary/driven): `LLMPort` implementation with prompt construction, intent classification, schedule summarization, error handling. | WireMock-based integration tests. Prompt format validation. Error → UNKNOWN mapping test. |
| 3.4 | `coagent-app` | Create `RestApiController` (primary/driving): user management endpoints, OAuth2 callback, schedule viewing, approval endpoints. Wire to inbound ports. Apply JWT validation + rate limiting. | Controller integration tests with MockMvc. |
| 3.5 | Integration wiring | Wire all modules together in Spring Boot application context. Verify DI resolves all port→adapter bindings. | Full context boot test. Smoke test hitting /actuator/health. |

**Exit Criterion:** All adapters implemented and tested with WireMock/Testcontainers. Full application context boots with all port→adapter wiring resolved. REST endpoints accept requests.

---

### Phase 4 — End-to-End Flows (Days 31–40)

**Goal:** Validate the two MVP use cases end-to-end.

| Step | Flow | Validation |
|------|------|------------|
| 4.1 | **Personal Calendar Management** | Slack message → IntentParser → ConflictDetector → Approval → Calendar write. Full cycle with real Slack dev workspace and Google test account. |
| 4.2 | **Collaborative Scheduling** | Agent A initiates → Coordination state machine → Agent B's availability → Proposal → Dual approval → Saga event creation → Both calendars updated. |
| 4.3 | Failure scenarios | Approval timeout (12h). Saga compensation (Agent B fails → Agent A event deleted). LLM unavailable (fallback to rule-based). Google Calendar unavailable (FAILED state). |
| 4.4 | GDPR flows | User deletion cascades correctly. Data export produces valid JSON. |

**Exit Criterion:** Both MVP use cases pass end-to-end with real external services. Failure scenarios trigger correct compensation/fallback behavior.

---

### Phase 5 — Production Hardening (Days 41–50)

**Goal:** Prepare for deployment.

| Step | Action | Validation |
|------|--------|------------|
| 5.1 | DockerFile (multi-stage build). Production-ready `docker-compose.yml`. | `docker build` produces image < 300MB. `docker-compose up` runs full stack. |
| 5.2 | Circuit breakers on all outbound adapters (Resilience4j). WebClient timeout + retry configuration. | Circuit breaker triggers on simulated failures. |
| 5.3 | Reconciliation scheduler: detect stuck intermediate saga states (>2 min), initiate compensation. | Integration test: force crash during saga, verify reconciliation recovers. |
| 5.4 | Approval timeout scheduler: batch scan every 15 min, `SKIP LOCKED`, expire overdue approvals. | Integration test: create approval, advance clock, verify expiration + domain event. |
| 5.5 | Structured JSON logging with correlation IDs. Health indicators for PostgreSQL, Slack, Google Calendar. | Log format validation. Health check reflects dependency status. |
| 5.6 | ArchUnit fitness functions: Agent Sovereignty enforcement, hexagonal dependency rules, no CalendarPort from coordination-module. | All ArchUnit tests pass in `mvn test`. |
| 5.7 | Security hardening: review HTTPS, CORS, cookie configuration, CSRF, input validation. OWASP dependency check. | Zero high/critical OWASP findings. |

**Exit Criterion:** Dockerized application runs in production-like configuration. All resilience patterns active. ArchUnit enforces architecture rules. Security checklist passed.

---

### Phase 6 — MVP Launch (Days 51–55)

| Step | Action |
|------|--------|
| 6.1 | Deploy to staging. Run Flyway migrations. Execute smoke tests against real Slack + Google Calendar. |
| 6.2 | Fix any issues found in staging. |
| 6.3 | Production deployment (manual approval gate). Rolling update. Verify health checks. |
| 6.4 | Monitor metrics: coordination duration P99, approval latency, saga compensation rate, circuit breaker state. |

---

## 2. Folder Structure

```
CoAgent4U/
├── pom.xml                                 # Root POM: multi-module reactor build
│
├── shared-kernel/                          # Pure Java — zero dependencies
│   ├── pom.xml
│   └── src/main/java/com/coagent4u/shared/
│       ├── UserId.java                     # record UserId(UUID value)
│       ├── AgentId.java
│       ├── CoordinationId.java
│       ├── ApprovalId.java
│       ├── TimeSlot.java
│       ├── TimeRange.java
│       ├── Email.java
│       ├── Duration.java
│       ├── EventId.java
│       ├── SlackUserId.java
│       └── WorkspaceId.java
│
├── common-domain/                          # Depends only on shared-kernel
│   ├── pom.xml
│   └── src/main/java/com/coagent4u/common/
│       ├── DomainEvent.java                # Marker interface
│       ├── DomainEventPublisher.java       # Port interface
│       └── events/
│           ├── UserRegistered.java
│           ├── UserDeleted.java
│           ├── AgentProvisioned.java
│           ├── PersonalEventCreated.java
│           ├── CoordinationStateChanged.java
│           ├── CoordinationCompleted.java
│           ├── CoordinationFailed.java
│           ├── ApprovalDecisionMade.java
│           └── ApprovalExpired.java
│
├── core/
│   ├── user-module/
│   │   ├── pom.xml                         # Depends: shared-kernel, common-domain
│   │   └── src/main/java/com/coagent4u/user/
│   │       ├── domain/
│   │       │   ├── User.java               # Aggregate root
│   │       │   ├── SlackIdentity.java
│   │       │   ├── ServiceConnection.java
│   │       │   └── UserConnectionStatus.java
│   │       ├── port/
│   │       │   ├── in/
│   │       │   │   ├── RegisterUserUseCase.java
│   │       │   │   ├── ConnectServiceUseCase.java
│   │       │   │   ├── DisconnectServiceUseCase.java
│   │       │   │   └── DeleteUserUseCase.java
│   │       │   └── out/
│   │       │       ├── UserPersistencePort.java
│   │       │       └── UserQueryPort.java
│   │       └── application/
│   │           └── UserManagementService.java
│   │
│   ├── agent-module/
│   │   ├── pom.xml                         # Depends: shared-kernel, common-domain
│   │   └── src/main/java/com/coagent4u/agent/
│   │       ├── domain/
│   │       │   ├── Agent.java              # Aggregate root
│   │       │   ├── IntentParser.java        # Rule-based Tier 1
│   │       │   ├── ConflictDetector.java
│   │       │   ├── ParsedIntent.java
│   │       │   └── IntentType.java
│   │       ├── port/
│   │       │   ├── in/
│   │       │   │   ├── HandleMessageUseCase.java
│   │       │   │   ├── ViewScheduleUseCase.java
│   │       │   │   └── CreatePersonalEventUseCase.java
│   │       │   └── out/
│   │       │       ├── CalendarPort.java     # Sole consumer — agent module only
│   │       │       ├── LLMPort.java
│   │       │       ├── ApprovalPort.java
│   │       │       ├── AgentPersistencePort.java
│   │       │       └── UserQueryPort.java    # Consumed from user-module
│   │       ├── capability/                   # Implements coordination's outbound ports
│   │       │   ├── AgentAvailabilityPortImpl.java
│   │       │   ├── AgentEventExecutionPortImpl.java
│   │       │   ├── AgentProfilePortImpl.java
│   │       │   └── AgentApprovalPortImpl.java
│   │       ├── handler/                      # Domain event handlers
│   │       │   ├── CollaborativeApprovalDecisionHandler.java
│   │       │   ├── CollaborativeApprovalExpiredHandler.java
│   │       │   ├── PersonalApprovalDecisionHandler.java
│   │       │   └── PersonalApprovalExpiredHandler.java
│   │       └── application/
│   │           └── AgentCommandService.java
│   │
│   ├── coordination-module/
│   │   ├── pom.xml                         # Depends: shared-kernel, common-domain
│   │   └── src/main/java/com/coagent4u/coordination/
│   │       ├── domain/
│   │       │   ├── Coordination.java       # Aggregate root
│   │       │   ├── CoordinationState.java   # Enum: 14 states
│   │       │   ├── CoordinationStateMachine.java
│   │       │   ├── AvailabilityMatcher.java
│   │       │   ├── ProposalGenerator.java
│   │       │   ├── EventCreationSaga.java
│   │       │   ├── AvailabilityBlock.java
│   │       │   ├── MeetingProposal.java
│   │       │   ├── EventConfirmation.java
│   │       │   └── CoordinationStateLogEntry.java
│   │       ├── port/
│   │       │   ├── in/
│   │       │   │   └── CoordinationProtocolPort.java  # Invoked by agent-module ONLY
│   │       │   └── out/
│   │       │       ├── AgentAvailabilityPort.java      # Implemented by agent-module
│   │       │       ├── AgentEventExecutionPort.java     # Implemented by agent-module
│   │       │       ├── AgentProfilePort.java            # Implemented by agent-module
│   │       │       ├── AgentApprovalPort.java           # Implemented by agent-module
│   │       │       └── CoordinationPersistencePort.java
│   │       └── application/
│   │           └── CoordinationService.java  # Implements CoordinationProtocolPort
│   │
│   └── approval-module/
│       ├── pom.xml                         # Depends: shared-kernel, common-domain
│       └── src/main/java/com/coagent4u/approval/
│           ├── domain/
│           │   ├── Approval.java           # Aggregate root
│           │   ├── ApprovalStatus.java
│           │   ├── ApprovalType.java
│           │   └── ExpirationPolicy.java
│           ├── port/
│           │   ├── in/
│           │   │   ├── CreateApprovalUseCase.java
│           │   │   └── DecideApprovalUseCase.java
│           │   └── out/
│           │       └── ApprovalPersistencePort.java
│           └── application/
│               └── ApprovalService.java
│
├── integration/
│   ├── calendar-module/
│   │   ├── pom.xml                         # Depends: shared-kernel (+ Google Calendar API client)
│   │   └── src/main/java/com/coagent4u/calendar/
│   │       └── GoogleCalendarAdapter.java  # Implements CalendarPort
│   │
│   ├── messaging-module/
│   │   ├── pom.xml                         # Depends: shared-kernel (+ Slack SDK)
│   │   └── src/main/java/com/coagent4u/messaging/
│   │       ├── SlackInboundAdapter.java    # Primary (driving) — webhook handler
│   │       └── SlackNotificationAdapter.java # Secondary (driven) — NotificationPort
│   │
│   └── llm-module/
│       ├── pom.xml                         # Depends: shared-kernel (+ WebClient)
│       └── src/main/java/com/coagent4u/llm/
│           └── GroqLLMAdapter.java         # Implements LLMPort
│
├── infrastructure/
│   ├── persistence/
│   │   ├── pom.xml                         # Depends: all core modules (port interfaces), Spring Data JPA
│   │   └── src/
│   │       ├── main/java/com/coagent4u/persistence/
│   │       │   ├── user/
│   │       │   │   ├── UserJpaEntity.java
│   │       │   │   ├── UserJpaRepository.java
│   │       │   │   ├── UserMapper.java
│   │       │   │   └── UserPersistenceAdapter.java   # Implements UserPersistencePort
│   │       │   ├── agent/
│   │       │   ├── coordination/
│   │       │   ├── approval/
│   │       │   └── audit/
│   │       └── main/resources/db/migration/
│   │           ├── V1__create_user_tables.sql
│   │           ├── V2__create_agent_tables.sql
│   │           ├── V3__create_coordination_tables.sql
│   │           ├── V4__create_approval_tables.sql
│   │           └── V5__create_audit_tables.sql
│   │
│   ├── security/
│   │   ├── pom.xml
│   │   └── src/main/java/com/coagent4u/security/
│   │       ├── JwtValidator.java
│   │       ├── JwtIssuer.java
│   │       ├── AesTokenEncryption.java
│   │       ├── SlackSignatureVerifier.java
│   │       ├── OAuth2PkceHandler.java
│   │       └── CaffeineRateLimiter.java
│   │
│   ├── config/
│   │   ├── pom.xml
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── monitoring/
│       ├── pom.xml
│       └── src/main/java/com/coagent4u/monitoring/
│           ├── SpringEventPublisherAdapter.java  # Implements DomainEventPublisher
│           ├── AgentActivityEventHandler.java
│           ├── MetricsEventHandler.java
│           └── StructuredLogHandler.java
│
├── coagent-app/                            # Spring Boot application — assembles all modules
│   ├── pom.xml                             # Depends: ALL modules
│   └── src/main/java/com/coagent4u/
│       └── CoAgent4UApplication.java       # @SpringBootApplication
│
├── docker-compose.yml
├── Dockerfile
└── .env.example
```

> **Key rule:** The `domain/` package in any core module has **zero** Spring, JPA, Slack, or Google imports. Only shared-kernel types are allowed. This is enforced by Maven dependency scope and ArchUnit.

---

## 3. Module Implementation Order

Implementation follows a strict bottom-up dependency order. No module is implemented until all its dependencies exist at the port-interface level.

```
Phase 0   ─────────────────────────────────────────────────────
           shared-kernel  →  common-domain  →  project skeleton

Phase 1a  ─────────────────────────────────────────────────────
           user-module (domain + ports + app service)
           approval-module (domain + ports + app service)

Phase 1b  ─────────────────────────────────────────────────────
           coordination-module (domain + ports + app service)
               ↳ declares AgentAvailabilityPort, AgentEventExecutionPort,
                  AgentProfilePort, AgentApprovalPort (interfaces only)

Phase 1c  ─────────────────────────────────────────────────────
           agent-module (domain + ports + app service + capability impls)
               ↳ implements AgentAvailabilityPortImpl, etc.
               ↳ implements event handlers

Phase 2   ─────────────────────────────────────────────────────
           persistence  →  security  →  config  →  monitoring

Phase 3   ─────────────────────────────────────────────────────
           messaging-module  →  calendar-module  →  llm-module

Phase 4   ─────────────────────────────────────────────────────
           coagent-app (assembly + REST controllers + wiring)
```

### Why This Order?

| Order Decision | Rationale | Source |
|----------------|-----------|--------|
| `shared-kernel` first | All modules depend on it. Zero-logic value objects. | arc42 §04 Rule 4 |
| `user-module` before `agent-module` | Agent depends on `UserQueryPort` (declared in user-module). | arc42 §05.2.3 |
| `approval-module` before `agent-module` | Agent depends on `ApprovalPort` (declared in approval-module). | arc42 §05.2.4 |
| `coordination-module` before `agent-module` | Coordination declares the agent capability port interfaces that agent-module implements. Agent-module also depends on `CoordinationProtocolPort` (declared in coordination-module). | ADR-03, ADR-13, ADR-14 |
| `persistence` after all core modules | Persistence adapters implement port interfaces declared in core modules. | Hexagonal Architecture (arc42 §04 S2.1) |
| Integration modules last | They implement outbound port interfaces. Domain must be complete first. | arc42 §04 S2.1 |

---

## 4. Dependency Strategy

### Maven Module Dependencies

```
shared-kernel          → (none)
common-domain          → shared-kernel
user-module            → shared-kernel, common-domain
approval-module        → shared-kernel, common-domain
coordination-module    → shared-kernel, common-domain
agent-module           → shared-kernel, common-domain, coordination-module*, user-module*, approval-module*
calendar-module        → shared-kernel
messaging-module       → shared-kernel
llm-module             → shared-kernel
persistence            → shared-kernel, common-domain, user-module, agent-module,
                         coordination-module, approval-module, Spring Data JPA
security               → (Spring Security, Caffeine)
monitoring             → shared-kernel, common-domain, (Spring Actuator, Micrometer)
coagent-app            → ALL modules
```

> \* `agent-module` depends on other core modules **only for port interfaces**, never for domain or application classes. The dependency is on the port interface JAR, not the implementation.

### Critical Dependency Rules (ArchUnit-Enforced)

| Rule ID | Rule | Enforcement |
|---------|------|-------------|
| DR-01 | `coordination-module` must **never** import from `calendar-module`, `user-module`, `approval-module`, or `messaging-module`. | Maven POM has no such dependency + ArchUnit |
| DR-02 | `coordination-module` must **never** import `CalendarPort`. | ArchUnit class-level rule |
| DR-03 | `coordination-module` must **never** import `LLMPort`. | ArchUnit — determinism enforcement |
| DR-04 | Only `agent-module` may invoke `CoordinationProtocolPort`. | ArchUnit caller restriction |
| DR-05 | Domain packages must have **zero** Spring, JPA, Slack, Google, or HTTP imports. | ArchUnit layer rule |
| DR-06 | No cross-module `PersistencePort` calls (e.g., `CoordinationService` cannot call `UserPersistencePort`). | ArchUnit package rule |
| DR-07 | Outbound ports return domain types only — never `HttpResponse`, `ResultSet`, `JsonNode`. | Code review + interface contract |

### Shared Kernel Scope Constraint

`shared-kernel` contains **only**:
- Immutable value objects (Java records)
- Constructor validation (non-null, format checks)
- Zero business logic

If a type contains a business rule, it belongs in a module's domain package — not shared-kernel.

---

## 5. Interface-First Development

Every module is built interface-first: **port interfaces are defined before any implementation code exists.**

### Development Sequence Per Module

```
1. Define domain value objects and aggregate structure (compile-only)
2. Define inbound port interfaces (use cases)
3. Define outbound port interfaces (persistence, notifications, capabilities)
4. Write domain service logic using port interfaces (pure Java, no DI framework)
5. Write application service (implements inbound ports, calls outbound ports)
6. Write unit tests using mock/stub implementations of outbound ports
7. LATER: Write adapter implementations (Phase 2-3)
```

### Port Interface Inventory

| Module | Port Name | Type | Direction |
|--------|-----------|------|-----------|
| **user-module** | `RegisterUserUseCase` | Inbound | ← driving adapters |
| | `ConnectServiceUseCase` | Inbound | ← driving adapters |
| | `DisconnectServiceUseCase` | Inbound | ← driving adapters |
| | `DeleteUserUseCase` | Inbound | ← driving adapters |
| | `UserPersistencePort` | Outbound | → persistence |
| | `UserQueryPort` | Outbound | → persistence (read-only) |
| **approval-module** | `CreateApprovalUseCase` | Inbound | ← agent-module |
| | `DecideApprovalUseCase` | Inbound | ← driving adapters |
| | `ApprovalPersistencePort` | Outbound | → persistence |
| **coordination-module** | `CoordinationProtocolPort` | Inbound | ← agent-module only |
| | `AgentAvailabilityPort` | Outbound (capability) | → agent-module |
| | `AgentEventExecutionPort` | Outbound (capability) | → agent-module |
| | `AgentProfilePort` | Outbound (capability) | → agent-module |
| | `AgentApprovalPort` | Outbound (capability) | → agent-module |
| | `CoordinationPersistencePort` | Outbound | → persistence |
| **agent-module** | `HandleMessageUseCase` | Inbound | ← Slack adapter |
| | `ViewScheduleUseCase` | Inbound | ← Slack/REST |
| | `CreatePersonalEventUseCase` | Inbound | ← Slack adapter |
| | `CalendarPort` | Outbound | → calendar-module |
| | `LLMPort` | Outbound | → llm-module |
| | `ApprovalPort` | Outbound | → approval-module |
| | `AgentPersistencePort` | Outbound | → persistence |
| | `UserQueryPort` | Outbound | → user-module |
| **common-domain** | `DomainEventPublisher` | Outbound | → monitoring |
| **shared** | `NotificationPort` | Outbound | → messaging-module |

> **Total: ~20 port interfaces** — This is the architectural cost of hexagonal purity. Each port is the swap-point for future adapter replacement or module extraction.

---

## 6. Database Evolution

### Migration Strategy

| Principle | Implementation |
|-----------|----------------|
| **Forward-only** | No rollback scripts. Corrections via new forward migration. |
| **Immutable** | Once merged, never edited. |
| **Module-scoped** | Each module's tables are created in its own migration file. |
| **Zero-downtime** | Add columns as nullable first → back-fill → add constraints in subsequent migration. |
| **Domain-driven** | Tables reflect domain aggregates. Schema is designed from domain model outward. |

### Migration Sequence

```
V1__create_user_tables.sql
  ├── users
  ├── slack_identities
  └── service_connections

V2__create_agent_tables.sql
  └── agents

V3__create_coordination_tables.sql
  ├── coordinations
  └── coordination_state_log

V4__create_approval_tables.sql
  └── approvals

V5__create_audit_tables.sql
  └── agent_activities

V6__create_indexes.sql
  ├── idx_slack_identities_user_id
  ├── idx_service_connections_user_id
  ├── idx_agents_user_id
  ├── idx_coordinations_requester_agent_id
  ├── idx_coordinations_invitee_agent_id
  ├── idx_coordinations_state
  ├── idx_coordination_state_log_coordination_id
  ├── idx_approvals_user_id
  ├── idx_approvals_coordination_id
  ├── idx_approvals_expires_at  (for scheduler scan)
  └── idx_agent_activities_user_id
```

### Cross-Module Reference Pattern

Cross-module references use **plain UUIDs without foreign keys**:

```sql
-- coordinations table stores agent IDs as plain UUIDs
requester_agent_id  UUID NOT NULL,     -- NO FK to agents table
invitee_agent_id    UUID NOT NULL,     -- NO FK to agents table

-- approvals table stores coordination reference as plain UUID
coordination_id     UUID,              -- NO FK to coordinations table (nullable for personal)
```

This preserves module independence at the database level — each module can be extracted with its tables without FK entanglement. Data consistency is enforced at the application layer through port contracts.

### JPA Configuration

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate          # NEVER auto/create/update — Flyway owns schema
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## 7. Testing Strategy

### Testing Pyramid

```
                    ▲
                   / \            E2E Tests (Phase 4)
                  / 2  \          Real Slack + Google Calendar
                 /  to   \        Manual + automated smoke
                / 5 tests \
               ─────────────
              /     \              Integration Tests (Phase 2-3)
             / 30 to  \           Testcontainers + WireMock
            / 50 tests  \         Adapter + persistence + controller
           ─────────────────
          /       \                 Unit Tests (Phase 1)
         / 100 to   \             Pure domain logic
        / 200 tests   \            Port mocking
       / zero framework \          < 1 second total
      ─────────────────────
```

### Test Categories

| Category | What | How | Where | Phase |
|----------|------|-----|-------|-------|
| **Domain Unit** | Aggregate lifecycle, state machine transitions, value object validation, domain service logic | Plain JUnit 5 + AssertJ. No Spring context. Mocked ports via manual stubs or Mockito. | `core/*/src/test/java/` | 1 |
| **Application Unit** | Use case orchestration, event publishing, port call sequences | JUnit 5 + Mockito. No Spring context. | `core/*/src/test/java/` | 1 |
| **State Machine Exhaustive** | All 14 states × valid transitions. Invalid transition rejection. | Property-based testing (jqwik or manual matrix). | `core/coordination-module/src/test/` | 1 |
| **Persistence Integration** | Repository CRUD, query correctness, Flyway migration compatibility | Testcontainers (PostgreSQL), `@DataJpaTest` | `infrastructure/persistence/src/test/` | 2 |
| **Adapter Integration** | External API communication, error mapping, retry behavior | WireMock for Slack/Google/Groq APIs. | `integration/*/src/test/` | 3 |
| **Controller Integration** | REST endpoint behavior, JWT validation, input validation | MockMvc + Spring Boot Test | `coagent-app/src/test/` | 3 |
| **Architecture Fitness** | Hexagonal rules, Agent Sovereignty, no cross-module imports | ArchUnit | `coagent-app/src/test/` | 5 |
| **End-to-End** | Full use case flows with real external services | Dev Slack workspace + Google test account + Ngrok | Manual + scripted | 4 |

### Key Testing Rules

| Rule | Rationale | Source |
|------|-----------|--------|
| Domain tests run in < 5 seconds total. | No I/O, no Spring context, no database. Pure Java. | arc42 §04 S2.1 |
| State machine tests cover **every** valid transition and reject **every** invalid transition. | Coordination correctness is the #1 quality goal (Determinism). | ADR-04 |
| Both orchestrator-driven and `CoordinationProtocolPort`-driven paths must be tested through the same state machine. | Dual entry points create divergence risk (R-ARCH-09). | ADR-13 |
| Agent capability ports are tested with mocked `CalendarPort` and `UserQueryPort`. | Verify agent delegation without external APIs. | ADR-03 |
| Saga tests verify: happy path, Agent A fails (TOTAL_FAILURE), Agent B fails + compensation (PARTIAL_FAILURE_COMPENSATED). | Three outcomes per ADR-05. | arc42 §05.2.2 |
| Integration tests use Testcontainers — **never** an embedded H2 database. | PostgreSQL-specific features (JSONB, `SELECT FOR UPDATE SKIP LOCKED`) must be tested against PostgreSQL. | TC-04 |

### Coverage Targets

| Layer | Target | Rationale |
|-------|--------|-----------|
| Domain | ≥ 90% line coverage | Core business logic. Bugs here corrupt coordination state. |
| Application | ≥ 80% line coverage | Orchestration logic. Misrouted commands cause user-visible errors. |
| Adapters | ≥ 70% line coverage | Translation logic. Tested primarily via integration tests. |
| Infrastructure | ≥ 60% line coverage | Security and config. Tested via integration tests + manual verification. |

---

## 8. CI/CD Approach

### Pipeline Stages

```
┌─────────┐    ┌─────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Build   │ ─→ │  Unit   │ ─→ │ Integr.  │ ─→ │ ArchUnit │ ─→ │  Static  │
│ (Maven)  │    │  Tests  │    │  Tests   │    │  Tests   │    │ Analysis │
└─────────┘    └─────────┘    └──────────┘    └──────────┘    └──────────┘
                                                                     │
                                                                     ▼
                                              ┌──────────┐    ┌──────────┐
                                              │ Staging   │ ←─ │  Docker  │
                                              │ Deploy +  │    │  Build   │
                                              │  Smoke    │    │          │
                                              └──────────┘    └──────────┘
                                                    │
                                                    ▼ Manual approval
                                              ┌──────────┐
                                              │Production │
                                              │  Deploy   │
                                              └──────────┘
```

| Stage | Tool | Gate Condition |
|-------|------|----------------|
| Build | Maven 3.9+, Java 21 | Zero compilation errors/warnings |
| Unit Tests | JUnit 5, Mockito, AssertJ | 100% pass. Coverage ≥ 80% domain/app layers. |
| Integration Tests | Testcontainers, WireMock, Spring Boot Test | 100% pass |
| Architecture Tests | ArchUnit | Zero violations (Agent Sovereignty, hexagonal rules) |
| Static Analysis | SpotBugs, OWASP Dependency Check | Zero high/critical findings |
| Docker Build | Multi-stage Dockerfile | Image < 300MB, builds successfully |
| Staging Deploy | docker-compose or orchestrator | Health check passes, Flyway succeeds |
| Smoke Tests | RestAssured or manual | Slack webhook → response, personal schedule → 200 |
| Production Deploy | Rolling update, manual approval | Health checks pass on all new instances |

### MVP Simplification

For the MVP as a solo developer:
- **CI Tool:** GitHub Actions (free tier is sufficient)
- **Production Deploy:** Manual `docker-compose up -d` on a single VPS, or a simple cloud container service (Cloud Run, Fargate)
- **No staging environment initially** — deploy straight to production after local testing passes. Add staging when confidence warrants the overhead.

---

## 9. Risk Mitigation

### Top Risks Addressed in Implementation Order

| Risk | Source | Phase Addressed | Mitigation |
|------|--------|-----------------|------------|
| **Agent module becomes "God module"** (R-ARCH-10) | arc42 §11 | Phase 1c | Strict internal sub-packaging: `domain/`, `port/`, `capability/`, `handler/`, `application/`. Each handler has single responsibility. If >30 classes, split into sub-modules. |
| **State machine dual-entry divergence** (R-ARCH-09) | ADR-13 | Phase 1b | Both `CoordinationService` (orchestrator) and `CoordinationProtocolPort` (agent-mediated) use identical `CoordinationStateMachine` domain service. Exhaustive transition matrix tests cover both paths. |
| **Saga compensation failure** (TD-09) | arc42 §11 | Phase 5 | Reconciliation scheduler: scan for stuck intermediate states (>2 min), auto-compensate via `AgentEventExecutionPort.deleteEvent()`. Log compensation failures for manual review. |
| **Approval timeout race condition** (R-ARCH-04) | ADR-07 | Phase 5 | `SELECT FOR UPDATE` with pessimistic locking. Timeout batch uses `SKIP LOCKED`. Lock acquisition timeout = 5 seconds. |
| **In-process events lost on crash** (R-ARCH-01) | ADR-06 | Accepted for MVP | Coordination state is committed to DB before events are published. Missed notifications are side-effects only. Reconciliation scheduler acts as implicit retry on restart. |
| **Single PostgreSQL SPOF** (R-ARCH-02) | ADR-09 | Phase 5 | Use managed PostgreSQL with automated failover. Health checks remove unhealthy app nodes. Accepted risk for MVP. |
| **External API latency** (R-ARCH-03) | ADR-05 | Phase 5 | Circuit breakers (Resilience4j) on all outbound adapters. Saga timeout caps total execution. |

### Architecture Enforcement

| Enforcement | Tool | When |
|-------------|------|------|
| Module boundary violations | Maven POM dependency scope | Compile time |
| Agent Sovereignty bypass | ArchUnit fitness functions | `mvn test` |
| Hexagonal layer violations | ArchUnit package rules | `mvn test` |
| Domain purity (no framework imports) | ArchUnit class-level rules | `mvn test` |
| No cross-module JOINs | Code review + ArchUnit | `mvn test` + PR review |

---

## 10. MVP Completion Definition

### Functional Criteria

| # | Criterion | Validation |
|---|-----------|------------|
| F1 | User registers via Slack and is provisioned with a personal agent. | Slack message triggers registration → agent record exists in DB. |
| F2 | User connects Google Calendar via OAuth2 PKCE flow. | OAuth callback → encrypted token stored → calendar queries work. |
| F3 | User asks agent to add a personal event via Slack → receives approval prompt → approves → event created in Google Calendar. | End-to-end with real Slack + Google Calendar. |
| F4 | User asks agent to schedule a meeting with another user → coordination state machine runs → both users receive approval prompts → both approve → events created in both calendars. | End-to-end collaborative flow. |
| F5 | Agent detects calendar conflicts and reports them before creating approval. | ConflictDetector finds overlapping events. |
| F6 | Approval times out after 12 hours → coordination rejected → users notified. | Scheduler processes expired approvals. |
| F7 | Saga compensation works: if Agent B's calendar fails, Agent A's event is deleted. | Simulated failure during saga. |
| F8 | User deletes account → all data cascaded (GDPR Right to Erasure). | Deletion procedure verified via integration test. |
| F9 | Two-tier intent parsing: rule-based handles common commands; LLM fallback for ambiguous. | IntentParser unit tests + integration test with Groq. |
| F10 | Web dashboard: user can view schedule, manage connections, decide approvals. | REST API endpoints return correct data. |

### Non-Functional Criteria

| # | Criterion | Target | Source |
|---|-----------|--------|--------|
| NF1 | Personal scheduling end-to-end latency | < 5 seconds (P95) | PRD NFR |
| NF2 | Coordination end-to-end latency (excl. human approval time) | < 10 seconds (P95) | PRD NFR |
| NF3 | Approval timeout precision | Within 15 minutes of 12-hour mark | ADR-11 |
| NF4 | ArchUnit tests pass: zero violations | 100% | DR-01 through DR-07 |
| NF5 | Domain layer: zero Spring/JPA/framework imports | Verified by ArchUnit | arc42 §04 S2.1 |
| NF6 | Domain + application layer test coverage | ≥ 80% | CI pipeline gate |
| NF7 | Docker image builds and runs | < 300MB, <30s startup | arc42 §07 |
| NF8 | All state machine transitions tested | 14 states, all valid + invalid | ADR-04 |
| NF9 | OWASP dependency check | Zero high/critical CVEs | CI pipeline gate |
| NF10 | Slack signature verification on every webhook | No unsigned requests processed | Security requirement |

### Explicitly Out of Scope for MVP

| Feature | Status | When |
|---------|--------|------|
| Multi-participant scheduling (>2 users) | Deferred | Post-MVP when state machine is versioned |
| Trust tiers / auto-approve | Deferred | After user trust is established |
| Multiple calendar providers (Outlook, Apple) | Deferred | Post-MVP via new CalendarPort adapters |
| LLM-based negotiation | Deferred | Non-deterministic, conflicts with Q1 |
| Transactional outbox | Deferred (TD-01) | When user count > 1,000 |
| Distributed cache (Redis) | Deferred (TD-02) | When cache hit ratio < 50% |
| Multi-region deployment | Out of scope | Requires fundamental re-architecture |
| Mobile app | Out of scope | Web + Slack covers MVP scenarios |

---

> **This roadmap is complete.** Implementation begins at Phase 0. Each phase has explicit exit criteria. Every decision traces back to the PRD, arc42, or C4 documentation. No alternative architectures are proposed — the roadmap follows the documented design exactly.
