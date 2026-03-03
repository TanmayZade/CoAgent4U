# 11. Risks

## Table of Contents

- [1. Risk Classification Model](#1-risk-classification-model)
  - [1.1 Severity Levels](#11-severity-levels)
  - [1.2 Likelihood Levels](#12-likelihood-levels)
  - [1.3 Impact Categories](#13-impact-categories)
  - [1.4 Risk Scoring Matrix](#14-risk-scoring-matrix)
- [2. Architectural Risks](#2-architectural-risks)
  - [R-ARCH-01 — In-Process Event Bus Has No Durability Guarantee](#r-arch-01--in-process-event-bus-has-no-durability-guarantee)
  - [R-ARCH-02 — Single PostgreSQL Database as Shared Failure Domain](#r-arch-02--single-postgresql-database-as-shared-failure-domain)
  - [R-ARCH-03 — Sequential Saga Execution Creates Latency Under Load](#r-arch-03--sequential-saga-execution-creates-latency-under-load)
  - [R-ARCH-04 — Pessimistic Locking Under High Concurrency](#r-arch-04--pessimistic-locking-under-high-concurrency)
  - [R-ARCH-05 — Coordination State Machine Rigidity](#r-arch-05--coordination-state-machine-rigidity)
  - [R-ARCH-06 — Correlation IDs Without Distributed Tracing](#r-arch-06--correlation-ids-without-distributed-tracing)
  - [R-ARCH-07 — Non-Distributed In-Memory Cache](#r-arch-07--non-distributed-in-memory-cache)
  - [R-ARCH-08 — Monolith Scaling Granularity](#r-arch-08--monolith-scaling-granularity)
  - [R-ARCH-09 — Dual Entry Points to State Machine Increase Testing Surface](#r-arch-09--dual-entry-points-to-state-machine-increase-testing-surface)
  - [R-ARCH-10 — Agent Module Scope Growth Due to Mediation Responsibilities](#r-arch-10--agent-module-scope-growth-due-to-mediation-responsibilities)
- [3. Technical Debt Inventory](#3-technical-debt-inventory)
- [4. Operational Risks](#4-operational-risks)
  - [R-OPS-01 — Database Outage as Single Point of Failure](#r-ops-01--database-outage-as-single-point-of-failure)
  - [R-OPS-02 — Circuit Breaker Misconfiguration](#r-ops-02--circuit-breaker-misconfiguration)
  - [R-OPS-03 — Slack Retry Storm](#r-ops-03--slack-retry-storm)
  - [R-OPS-04 — Connection Pool Exhaustion](#r-ops-04--connection-pool-exhaustion)
  - [R-OPS-05 — Log Volume Explosion](#r-ops-05--log-volume-explosion)
  - [R-OPS-06 — Scheduler Overload Under Burst Expiration](#r-ops-06--scheduler-overload-under-burst-expiration)
  - [R-OPS-07 — JWT Secret Mismatch Across Nodes](#r-ops-07--jwt-secret-mismatch-across-nodes)
  - [R-OPS-08 — Agent-Mediated Event Handler Failure Silent Drop](#r-ops-08--agent-mediated-event-handler-failure-silent-drop)
- [5. Scaling & Growth Risks](#5-scaling--growth-risks)
  - [R-SCALE-01 — 10× User Growth](#r-scale-01--10-user-growth)
  - [R-SCALE-02 — 20× Coordination Volume](#r-scale-02--20-coordination-volume)
  - [R-SCALE-03 — External API Latency Increase](#r-scale-03--external-api-latency-increase)
  - [R-SCALE-04 — Multi-Region Deployment](#r-scale-04--multi-region-deployment)
  - [R-SCALE-05 — Agent Module Extraction to Microservice](#r-scale-05--agent-module-extraction-to-microservice)
  - [R-SCALE-06 — Multiple Calendar Providers](#r-scale-06--multiple-calendar-providers)
- [6. Security & Compliance Risks](#6-security--compliance-risks)
  - [R-SEC-01 — OAuth Token Exposure](#r-sec-01--oauth-token-exposure)
  - [R-SEC-02 — AES Key Leakage](#r-sec-02--aes-key-leakage)
  - [R-SEC-03 — Incomplete GDPR Deletion](#r-sec-03--incomplete-gdpr-deletion)
  - [R-SEC-04 — Correlation ID Leaking PII](#r-sec-04--correlation-id-leaking-pii)
  - [R-SEC-05 — Log Retention Misconfiguration](#r-sec-05--log-retention-misconfiguration)
  - [R-SEC-06 — JWT Secret Compromise](#r-sec-06--jwt-secret-compromise)
  - [R-SEC-07 — Clock Skew Affecting Signature Validation](#r-sec-07--clock-skew-affecting-signature-validation)
  - [R-SEC-08 — Agent Sovereignty Bypass via Direct Module Access](#r-sec-08--agent-sovereignty-bypass-via-direct-module-access)
- [7. Risk Mitigation Strategy](#7-risk-mitigation-strategy)
  - [7.1 Preventive Controls](#71-preventive-controls)
  - [7.2 Detective Controls](#72-detective-controls)
  - [7.3 Compensating Controls](#73-compensating-controls)
  - [7.4 Refactoring Roadmap](#74-refactoring-roadmap)
  - [7.5 Architecture Evolution Decision Thresholds](#75-architecture-evolution-decision-thresholds)
- [8. Risk Monitoring & Early Warning Indicators](#8-risk-monitoring--early-warning-indicators)
- [9. Risk Review Cadence](#9-risk-review-cadence)

---

## 1. Risk Classification Model

### 1.1 Severity Levels

| Severity | Definition |
|----------|------------|
| Critical | Data loss, silent corruption of coordination state, or complete system unavailability. Requires immediate engineering response. |
| High | Degraded coordination correctness, partial outage affecting active users, or compliance violation. Requires resolution within 24 hours. |
| Medium | Reduced throughput, increased latency beyond SLO, or developer velocity impediment. Scheduled for next sprint. |
| Low | Cosmetic, minor inefficiency, or theoretical risk with no observed manifestation. Tracked in backlog. |

---

### 1.2 Likelihood Levels

| Likelihood | Definition |
|------------|------------|
| Almost Certain | Expected under normal production conditions within 30 days. |
| Likely | Expected under moderate load or partial outage within 90 days. |
| Possible | Plausible under edge-case conditions or growth scenarios within 12 months. |
| Unlikely | Requires multiple concurrent failures or adversarial action. |
| Rare | Theoretical; no known trigger path in current deployment model. |

---

### 1.3 Impact Categories

| Category | Description |
|----------|-------------|
| Data Integrity | Corruption or loss of coordination state, calendar events, or agent preferences. |
| Availability | System downtime or inability to process coordinations. |
| Compliance | GDPR violation, audit failure, or token exposure. |
| Cost | Uncontrolled LLM spend, infrastructure over-provisioning, or incident response cost. |
| Developer Velocity | Ability to ship features, onboard contributors, or refactor modules. |

---

### 1.4 Risk Scoring Matrix

| Likelihood \ Severity | Critical | High | Medium | Low |
|-----------------------|----------|------|--------|-----|
| Almost Certain | 25 | 20 | 15 | 10 |
| Likely | 20 | 16 | 12 | 8 |
| Possible | 15 | 12 | 9 | 6 |
| Unlikely | 10 | 8 | 6 | 4 |
| Rare | 5 | 4 | 3 | 2 |

Scores of 15 and above require active mitigation with a named owner. Scores of 20 and above block release until mitigated or accepted by the architecture board.

---

## 2. Architectural Risks

### R-ARCH-01 — In-Process Event Bus Has No Durability Guarantee

| Attribute | Detail |
|-----------|--------|
| Description | Domain events published via Spring `ApplicationEventPublisher` exist only in-process memory. If the JVM crashes after a database commit but before all event listeners complete, downstream side effects (notifications via messaging-module's `NotificationEventHandler`, cache invalidation) are silently lost. This risk is amplified by the introduction of `CoordinationProtocolPort` (ADR-13): when an agent-mediated approval decision triggers a domain event (e.g., `CoordinationCompleted`), the event-driven notification path through messaging-module depends on the in-process bus surviving long enough to deliver the event. |
| Trigger Conditions | JVM crash, OOM kill, or forced container termination during event fan-out — particularly between the `CoordinationProtocolPort.advance()` commit and the `NotificationEventHandler`'s `@TransactionalEventListener(AFTER_COMMIT)` execution. |
| Impact | Medium — Notifications not sent; cache stale. Coordination state itself is consistent because the aggregate write committed. No data corruption, but user-visible side effects missed. |
| Likelihood | Unlikely — Requires crash within the millisecond window between commit and listener completion. |
| Risk Score | 8 |
| Current Mitigation | Coordination state is the source of truth. Missed notifications are detectable via observability (notification-sent counter divergence). Scheduler re-evaluates pending states on restart, acting as an implicit retry. The `trigger_source` field in `coordination_state_log` (QS-A1) allows reconciliation to identify which terminal-state coordinations have not yet emitted their notification side effects. |
| Future Mitigation Path | Introduce a transactional outbox table. Events written in the same transaction as the aggregate. A polling publisher or CDC connector drains the outbox. This converts the in-process bus into an at-least-once delivery mechanism without adding an external broker. See also TD-01. |
| ADR Reference | ADR-06 |

---

### R-ARCH-02 — Single PostgreSQL Database as Shared Failure Domain

| Attribute | Detail |
|-----------|--------|
| Description | All modules — coordination, agent, approval, messaging, calendar integration — share a single PostgreSQL instance. A database outage is a total system outage. This risk is unchanged by ADR-13 and ADR-14; both `CoordinationProtocolPort` and `AgentApprovalPort` ultimately persist state to the same database. |
| Trigger Conditions | Hardware failure, cloud provider zone outage, storage exhaustion, long-running lock contention causing cascade. |
| Impact | Critical — Complete unavailability. No coordination can progress, no commands accepted. `CoordinationProtocolPort.advance()` and `AgentApprovalPort.createApprovalRequest()` both fail. |
| Likelihood | Unlikely — Managed PostgreSQL services provide HA with automated failover. |
| Risk Score | 10 |
| Current Mitigation | Managed PostgreSQL with synchronous replication and automated failover (RTO < 60s). Connection pool configured with validation queries. Health check probes detect connectivity loss and remove nodes from load balancer. |
| Future Mitigation Path | Read replicas for query-heavy paths (availability checks). If modules are extracted to services, each service owns its database, eliminating the shared failure domain. |
| ADR Reference | ADR-09 |

---

### R-ARCH-03 — Sequential Saga Execution Creates Latency Under Load

| Attribute | Detail |
|-----------|--------|
| Description | The dual-calendar saga creates Event A via `AgentEventExecutionPort`, then Event B via `AgentEventExecutionPort`, sequentially. Each step involves an external HTTP call to Google Calendar API mediated through the agent sovereignty boundary. Under load, saga duration compounds. |
| Trigger Conditions | Google Calendar API latency > 2s per call. Burst of coordinations reaching the `APPROVED` state (after both approvals via `CoordinationProtocolPort.advance()`) simultaneously. |
| Impact | Medium — End-to-end coordination duration increases. User-perceived delay between approval and calendar confirmation. |
| Likelihood | Possible — Google Calendar API P99 latency spikes are documented. |
| Risk Score | 9 |
| Current Mitigation | Circuit breaker on calendar adapter (inside agent-module's `CalendarPort` implementation). Saga timeout caps total execution. Failed sagas are retryable from the last committed step via reconciliation scheduler. |
| Future Mitigation Path | Parallelize the two calendar event creations where Agent Sovereignty allows (each agent's `CalendarPort` invoked concurrently via `AgentEventExecutionPort`). Requires careful compensation ordering: if both calls are in flight and one fails, the other must be compensated. |
| ADR Reference | ADR-05 |

---

### R-ARCH-04 — Pessimistic Locking Under High Concurrency

| Attribute | Detail |
|-----------|--------|
| Description | `SELECT ... FOR UPDATE` on coordination rows serializes all concurrent operations on the same coordination. Under extreme contention (many participants or systems acting on the same coordination simultaneously), lock wait times increase. With ADR-13 (`CoordinationProtocolPort`), there are now two distinct entry points to the state machine — orchestrator-driven and agent-mediated — both acquiring the same pessimistic lock on the coordination row. This increases the probability of lock contention during the approval-vs-timeout race window. |
| Trigger Conditions | More than 5 concurrent state transition attempts on a single coordination row. The most likely contention scenario is the approval-vs-timeout race: `CollaborativeApprovalDecisionHandler` calling `CoordinationProtocolPort.advance()` concurrently with `CollaborativeApprovalExpiredHandler` calling `CoordinationProtocolPort.terminate()`. |
| Impact | Medium — Increased latency for contended coordinations. No correctness risk — the pessimistic lock guarantees exactly one transition wins. |
| Likelihood | Unlikely — A single coordination typically involves 2 participants. The approval-vs-timeout race is the primary contention source and occurs within a narrow time window around the 12-hour expiry boundary. |
| Risk Score | 6 |
| Current Mitigation | Lock acquisition timeout configured at 5 seconds. Failed acquisitions return a retryable error. Timeout scheduler uses `SKIP LOCKED` for batch approval processing, so it never blocks on already-locked rows. `CoordinationProtocolPort.advance()` and `terminate()` both acquire the same lock, ensuring serialization without deadlock (single lock target per coordination). |
| Future Mitigation Path | If contention data shows degradation, evaluate optimistic locking with version columns for read-heavy paths, reserving pessimistic locks for state transitions only. |
| ADR Reference | ADR-04, ADR-07, ADR-13 |

---

### R-ARCH-05 — Coordination State Machine Rigidity

| Attribute | Detail |
|-----------|--------|
| Description | The state machine defines a fixed set of states and transitions at compile time. Adding a new coordination workflow (e.g., multi-round negotiation, tentative holds, multi-participant approval chains) requires modifying the state machine, which is a core domain artifact. With `CoordinationProtocolPort` (ADR-13) formalizing agent-mediated state advancement, changes to the state machine now also affect the protocol port's contract expectations — agents calling `advance()` or `terminate()` depend on specific state transition semantics. |
| Trigger Conditions | Product requirement for a coordination flow not expressible in the current state graph. |
| Impact | Medium — Developer velocity reduction. Risk of introducing bugs in existing flows when modifying the state machine. Additional risk of breaking agent-mediated advancement patterns that depend on current state semantics. |
| Likelihood | Possible — Business requirements evolve. |
| Risk Score | 9 |
| Current Mitigation | State machine is covered by exhaustive property-based tests that verify all valid transitions and reject invalid ones. Any modification must pass the full transition matrix. Integration tests verify both orchestrator-driven and `CoordinationProtocolPort`-driven paths through the same state machine. |
| Future Mitigation Path | Introduce state machine versioning. New coordination instances are created with a version tag. Multiple state machine definitions coexist, selected at instantiation. Legacy coordinations continue on their original version. `CoordinationProtocolPort` implementations version-gate their advance/terminate behavior accordingly. |
| ADR Reference | ADR-04, ADR-13 |

---

### R-ARCH-06 — Correlation IDs Without Distributed Tracing

| Attribute | Detail |
|-----------|--------|
| Description | Cross-module observability relies on correlation IDs propagated through MDC. There is no distributed tracing backend (no Jaeger, no Zipkin). Tracing a coordination across module boundaries — particularly through the agent sovereignty boundary where `CoordinationProtocolPort` calls originate from agent-module event handlers — requires manual log correlation. The `trigger_source` field (QS-A1) aids auditability but does not replace distributed trace visualization. |
| Trigger Conditions | Incident investigation involving cross-module event flow, particularly debugging why a `CoordinationProtocolPort.advance()` call did or did not fire after an approval decision. |
| Impact | Low — Debugging is slower but not impossible. Correlation IDs are present in all log entries, including those from `CoordinationProtocolPort` calls. |
| Likelihood | Likely — Incidents occur. |
| Risk Score | 12 |
| Current Mitigation | Structured JSON logging with correlation ID, module name, coordination ID, and `trigger_source` in every log entry. Log aggregation via centralized logging (ELK or equivalent). `CoordinationProtocolPort` calls propagate the correlation ID from the originating approval event via `EventContext`. |
| Future Mitigation Path | Integrate OpenTelemetry SDK. Spring Boot 3.x has native support. Export spans to a tracing backend. Minimal code change required because correlation IDs already exist. Agent-mediated flows through `CoordinationProtocolPort` would appear as child spans of the originating approval event span. |
| ADR Reference | ADR-11 |

---

### R-ARCH-07 — Non-Distributed In-Memory Cache

| Attribute | Detail |
|-----------|--------|
| Description | Caffeine caches are local to each JVM. Under horizontal scaling, cache state diverges across nodes. A cache invalidation on Node A does not propagate to Node B. |
| Trigger Conditions | Agent updates preferences on Node A. Subsequent request routed to Node B serves stale cached data. |
| Impact | Low — Cache is explicitly non-authoritative (ADR-10). All writes go through the database. Stale reads resolve on TTL expiry. |
| Likelihood | Almost Certain — Expected behavior under horizontal scaling. |
| Risk Score | 10 |
| Current Mitigation | Short TTLs (60–120 seconds). Cache used only for read-heavy, non-critical paths (agent preference lookups). Coordination state is never cached. |
| Future Mitigation Path | If staleness becomes user-visible, introduce Redis as a shared cache layer or use database-level LISTEN/NOTIFY for targeted invalidation. |
| ADR Reference | ADR-10 |

---

### R-ARCH-08 — Monolith Scaling Granularity

| Attribute | Detail |
|-----------|--------|
| Description | Horizontal scaling deploys the entire application. A spike in calendar-heavy load scales all modules, including the idle messaging adapter. |
| Trigger Conditions | Load concentrated in one module. |
| Impact | Low — Over-provisioning cost. No correctness impact. |
| Likelihood | Possible — Load patterns are uneven. |
| Risk Score | 6 |
| Current Mitigation | Application footprint is small (Spring Boot, single JAR). Over-provisioning cost is marginal compared to operational simplicity. |
| Future Mitigation Path | Module extraction to independent services when per-module scaling ROI justifies the distributed system complexity. Hexagonal boundaries and formalized inter-module ports (`CoordinationProtocolPort`, `AgentApprovalPort`, `AgentAvailabilityPort`, `AgentEventExecutionPort`, `AgentProfilePort`) are designed for this transition. |
| ADR Reference | ADR-01, ADR-02, ADR-13, ADR-14 |

---

### R-ARCH-09 — Dual Entry Points to State Machine Increase Testing Surface

| Attribute | Detail |
|-----------|--------|
| Description | With `CoordinationProtocolPort` (ADR-13), the coordination state machine now has two distinct entry points: the orchestrator-driven path (used for availability checking, proposal generation, saga execution) and the agent-mediated path (used for approval decisions and expiration events). Both paths call the same `CoordinationStateMachine` domain service with the same pessimistic locking, but the existence of two callers increases the testing surface and creates a risk of divergent behavior if one path is modified without updating the other. |
| Trigger Conditions | Developer modifies orchestrator logic (e.g., adds a new pre-saga validation step) without verifying that agent-mediated paths through `CoordinationProtocolPort` remain compatible. Or vice versa: new approval flow behavior expected by `CoordinationProtocolPort.advance()` is not reflected in orchestrator assumptions. |
| Impact | Medium — Potential for state machine inconsistency between the two paths, leading to coordinations stuck in unexpected states. No data corruption (pessimistic lock prevents dual-write), but functional correctness risk. |
| Likelihood | Possible — Dual-path complexity is inherent to the design. |
| Risk Score | 9 |
| Current Mitigation | Both paths use the identical `CoordinationStateMachine` domain service — there is no code duplication. Property-based state machine tests verify all transitions regardless of entry point. Integration tests explicitly cover concurrent orchestrator + `CoordinationProtocolPort` access (QS-D1, QS-D5). The `trigger_source` field in audit logs (QS-A1) makes it visible which path initiated each transition, aiding incident diagnosis. |
| Future Mitigation Path | Introduce a state machine test harness that generates all valid interleaving sequences of orchestrator-driven and agent-mediated transitions, verifying that every interleaving reaches a valid terminal state. This extends the current property-based tests from single-path to multi-path coverage. |
| ADR Reference | ADR-04, ADR-13 |

---

### R-ARCH-10 — Agent Module Scope Growth Due to Mediation Responsibilities

| Attribute | Detail |
|-----------|--------|
| Description | Agent-module now mediates all cross-module interactions on behalf of users: calendar operations (`CalendarPort`), approval creation (`AgentApprovalPort` → `ApprovalPort`, ADR-14), notification delivery (`NotificationPort`), and coordination state advancement (`CoordinationProtocolPort`, ADR-13). This concentration of mediation responsibilities makes agent-module the largest and most complex module, increasing the risk that it becomes a "God module" that is difficult to understand, test, and maintain. |
| Trigger Conditions | Addition of new agent capability ports (e.g., `AgentTaskPort`, `AgentPreferencePort`) further expanding agent-module's scope. Developer difficulty understanding the module's responsibilities. |
| Impact | Medium — Developer velocity reduction. Higher defect density in agent-module due to complexity. |
| Likelihood | Possible — Each new feature touching user-scoped operations naturally gravitates toward agent-module. |
| Risk Score | 9 |
| Current Mitigation | Agent-module's internal structure is organized into clear sub-packages: handlers (event-driven logic), ports (inbound and outbound interfaces), and adapters (implementations). Each handler has a single responsibility (e.g., `CollaborativeApprovalDecisionHandler` handles only approval-decision-to-coordination-advancement). ArchUnit tests enforce that handlers do not call each other. |
| Future Mitigation Path | If agent-module exceeds 30 classes or 3,000 LOC, evaluate splitting into sub-modules (agent-calendar, agent-approval, agent-coordination) within the monolith, each with its own port set. These sub-modules would share the agent sovereignty principle but have independent compilation units. |
| ADR Reference | ADR-03, ADR-13, ADR-14 |

---

## 3. Technical Debt Inventory

| Debt ID | Description | Classification | Impact Area | Effort | Priority |
|---------|-------------|----------------|-------------|--------|----------|
| TD-01 | No transactional outbox. Domain events are fire-and-forget in-process. A JVM crash between commit and listener execution silently drops side effects. Risk is amplified by `CoordinationProtocolPort` (ADR-13) triggering domain events (`CoordinationCompleted`, `CoordinationRejected`) that drive notification delivery through messaging-module. | Intentional (strategic) | Reliability | Medium | High — implement post-MVP when user count exceeds 1,000 |
| TD-02 | No distributed cache. Caffeine is local per node. Cache coherence relies entirely on TTL expiry. | Intentional (strategic) | Consistency | Low | Low — acceptable while cache is non-authoritative |
| TD-03 | Custom saga implementation instead of a workflow engine. Saga steps, compensation, and retry logic are hand-coded in the coordination module. Saga invokes `AgentEventExecutionPort` for each step. | Intentional (strategic) | Developer Velocity | High | Medium — evaluate Temporal or custom DSL if saga complexity grows beyond 4 steps |
| TD-04 | Manual AES-256-GCM key rotation. No automated key rotation pipeline. Operator must update the encryption key and re-encrypt stored tokens manually. | Deferred | Security, Compliance | Medium | High — automate before production compliance audit |
| TD-05 | Rate-limit counters are in-memory (Caffeine). On restart, counters reset. On horizontal scaling, each node has independent limits. | Intentional (strategic) | Security | Low | Medium — move to Redis or database-backed counters if abuse is observed |
| TD-06 | LLM fallback is inherently non-deterministic. Same input may produce different intents across invocations. | Intentional (strategic) | Determinism | Medium | Medium — mitigate with temperature=0, structured output schemas, and validation against allowed intent enum |
| TD-07 | Sequential availability checks. When checking multiple agents' availability via `AgentAvailabilityPort`, each agent's `CalendarPort` is invoked sequentially. | Accidental | Performance | Low | Low — parallelize with virtual threads (Java 21) when availability check latency exceeds SLO |
| TD-08 | Hard-coded scheduler intervals. Timeout scan interval and retry delays are compile-time constants. | Accidental | Operability | Low | Low — externalize to configuration properties |
| TD-09 | Manual reconciliation for compensation failure. If a saga compensation step fails (e.g., calendar delete via `AgentEventExecutionPort` fails), the system logs the failure and transitions to `REQUIRES_MANUAL_INTERVENTION` but requires operator intervention to clean up the orphaned event. | Deferred | Reliability | Medium | High — implement automated reconciliation job that retries failed compensations via `AgentEventExecutionPort` |
| TD-10 | `CoordinationProtocolPort` advance/terminate methods accept raw state transition parameters. No dedicated command objects. If the protocol evolves (e.g., adding reason codes, metadata, or multi-step advancement), method signatures must change, breaking agent-module callers. | Intentional (strategic) | Developer Velocity, Modularity | Low | Low — introduce command/result objects when protocol port method signatures exceed 3 parameters |
| TD-11 | Agent-module handlers (`CollaborativeApprovalDecisionHandler`, `CollaborativeApprovalExpiredHandler`) contain inline logic for mapping approval events to `CoordinationProtocolPort` calls. No domain service layer exists within agent-module to encapsulate this mediation logic. | Accidental | Testability, Developer Velocity | Low | Medium — extract agent-mediation domain service when agent-module exceeds 20 handler classes |

---

## 4. Operational Risks

### R-OPS-01 — Database Outage as Single Point of Failure

| Attribute | Detail |
|-----------|--------|
| Trigger | Cloud provider zone failure, storage corruption, misconfigured failover. |
| Impact | Critical — Total system outage. Both orchestrator-driven and agent-mediated (`CoordinationProtocolPort`) paths require database access. |
| Likelihood | Unlikely |
| Risk Score | 10 |
| Mitigation | Managed PostgreSQL with multi-AZ replication. Automated failover with RTO < 60s. Daily backups with point-in-time recovery. Connection pool validation on borrow. Health check probe fails immediately, removing nodes from load balancer. |

---

### R-OPS-02 — Circuit Breaker Misconfiguration

| Attribute | Detail |
|-----------|--------|
| Trigger | Threshold set too high: circuit never opens during degradation. Threshold set too low: circuit opens on transient errors, causing unnecessary failures. |
| Impact | High — Either cascading failure or false-positive outage. Affects all agent-mediated operations that transit through `CalendarPort` (availability checks via `AgentAvailabilityPort`, event creation via `AgentEventExecutionPort`). |
| Likelihood | Possible |
| Risk Score | 12 |
| Mitigation | Circuit breaker parameters (failure rate threshold, slow call threshold, wait duration in open state) are externalized to configuration. Load testing validates thresholds before release. Circuit breaker state transitions emit Micrometer events for alerting (QS-O3). |

---

### R-OPS-03 — Slack Retry Storm

| Attribute | Detail |
|-----------|--------|
| Trigger | Application returns non-200 to Slack webhook. Slack retries up to 3 times with exponential backoff. If multiple interactions fail, retry volume amplifies. |
| Impact | Medium — Duplicate processing attempts. No data corruption due to idempotency checks, but increased load. |
| Likelihood | Possible |
| Risk Score | 9 |
| Mitigation | Idempotency key derived from Slack interaction payload ID. Duplicate requests short-circuit with 200 response. Rate limiting on the Slack webhook endpoint. |

---

### R-OPS-04 — Connection Pool Exhaustion

| Attribute | Detail |
|-----------|--------|
| Trigger | Slow queries, long-held pessimistic locks, or burst of concurrent coordinations exhaust HikariCP pool. Risk slightly increased by `CoordinationProtocolPort` (ADR-13): agent-mediated state transitions hold pessimistic locks during the same connection pool, competing with orchestrator-driven transitions and scheduler batch processing. |
| Impact | High — New requests cannot acquire connections. Cascading timeouts. Both orchestrator and protocol port paths blocked. |
| Likelihood | Unlikely |
| Risk Score | 8 |
| Mitigation | Pool sized to 2× expected concurrent coordination count. Connection acquisition timeout set to 5 seconds. Leak detection enabled (log warning if connection held > 30 seconds). Micrometer exports pool utilization metrics. Alert at 80% utilization. `CoordinationProtocolPort` transactions are short-lived (P99 < 200ms per QS-P6), limiting lock hold time. |

---

### R-OPS-05 — Log Volume Explosion

| Attribute | Detail |
|-----------|--------|
| Trigger | DEBUG-level logging left enabled in production. External API retry loops generating excessive log entries. Additional log volume from `trigger_source` tracking in `coordination_state_log` (QS-A1). |
| Impact | Medium — Log storage cost increase. Log search performance degradation. |
| Likelihood | Possible |
| Risk Score | 9 |
| Mitigation | Production log level set to INFO. Per-package log level overrides via configuration. Log sampling for high-frequency events (e.g., health checks). Log rotation and retention policy enforced at infrastructure level. Audit log entries (`coordination_state_log`) are written to the database, not the log stream, limiting their contribution to log volume. |

---

### R-OPS-06 — Scheduler Overload Under Burst Expiration

| Attribute | Detail |
|-----------|--------|
| Trigger | Large batch of coordinations created simultaneously all expire at the same time. Scheduler scan picks up hundreds of rows in one cycle. Each expired approval with coordination type = COLLABORATIVE triggers a `CoordinationProtocolPort.terminate()` call (via `CollaborativeApprovalExpiredHandler`), which acquires a pessimistic lock on the coordination row — compounding processing time. |
| Impact | Medium — Scheduler cycle duration exceeds interval. Temporary processing delay. |
| Likelihood | Possible |
| Risk Score | 9 |
| Mitigation | `SKIP LOCKED` ensures multiple nodes share the approval expiration load. Scheduler processes rows in configurable batch sizes (default 50). Jitter added to expiration times at creation (+/- 30 seconds) to prevent exact alignment. `CoordinationProtocolPort.terminate()` transactions are short-lived, limiting lock hold time per coordination. |

---

### R-OPS-07 — JWT Secret Mismatch Across Nodes

| Attribute | Detail |
|-----------|--------|
| Trigger | Rolling deployment where new nodes receive an updated JWT secret while old nodes still use the previous one. |
| Impact | High — Tokens signed by one node rejected by another. Intermittent authentication failures. |
| Likelihood | Unlikely |
| Risk Score | 8 |
| Mitigation | JWT secret injected via environment variable from a single secret store. Rolling deployment strategy ensures all nodes converge on the same secret. Support for multiple valid signing keys during rotation window (JWKS-style key set). |

---

### R-OPS-08 — Agent-Mediated Event Handler Failure Silent Drop

| Attribute | Detail |
|-----------|--------|
| Trigger | `CollaborativeApprovalDecisionHandler` or `CollaborativeApprovalExpiredHandler` in agent-module throws an unhandled exception during `CoordinationProtocolPort.advance()` or `terminate()` call. Because these handlers are invoked via in-process domain events (ADR-06), the failure is logged but not retried. |
| Impact | High — Coordination remains stuck in the awaiting-approval state. The approval decision was processed (approval entity updated), but the coordination was never advanced. Users see a "hanging" coordination that neither completes nor times out. |
| Likelihood | Unlikely — `CoordinationProtocolPort` calls are simple pessimistic-lock + state-transition operations with well-tested guard conditions. |
| Risk Score | 8 |
| Current Mitigation | Reconciliation scheduler detects coordinations stuck in awaiting-approval states beyond 2× the approval timeout (24 hours) and transitions them to `REQUIRES_MANUAL_INTERVENTION`. Error logging from the handler includes correlation ID and coordination ID for incident diagnosis. |
| Future Mitigation Path | Transactional outbox (TD-01) makes event delivery at-least-once, ensuring the handler is retried. Alternatively, the reconciliation scheduler could actively re-derive the expected state from the approval entity's current status and advance the coordination accordingly. |
| ADR Reference | ADR-06, ADR-13 |

---

## 5. Scaling & Growth Risks

### R-SCALE-01 — 10× User Growth

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| Database connections | Pool exhaustion at ~200 concurrent coordinations per node with default pool size of 20. Both orchestrator and `CoordinationProtocolPort` paths compete for the same pool. | Add read replicas for query paths. Increase pool size or add nodes. |
| Caffeine cache hit ratio | More unique agents reduce cache effectiveness. | Monitor hit ratio. If below 60%, evaluate shared cache (Redis). |
| Slack webhook throughput | Higher inbound message volume. | Horizontal scaling handles this linearly. No single-node bottleneck. |
| Agent-module handler throughput | More approval decisions and expirations flowing through `CollaborativeApprovalDecisionHandler`/`ExpiredHandler` → `CoordinationProtocolPort`. | Monitor `CoordinationProtocolPort` call metrics (QS-O5). If no-op rate rises above 5% or error rate above 1%, investigate. |

---

### R-SCALE-02 — 20× Coordination Volume

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| Pessimistic lock contention | More concurrent transitions on overlapping coordination rows. Two entry points (orchestrator + `CoordinationProtocolPort`) increase contention probability. | Monitor lock wait time P99. If > 1s, evaluate partitioning strategy. |
| Saga execution throughput | Sequential external calls via `AgentEventExecutionPort` become the bottleneck. | Parallelize calendar API calls per agent. Introduce saga execution thread pool with bounded queue. |
| Scheduler scan duration | More rows to evaluate per cycle. Each expired collaborative approval triggers a `CoordinationProtocolPort.terminate()` call. | `SKIP LOCKED` distributes across nodes. If scan duration > interval, reduce batch size and increase node count. |
| `AgentApprovalPort` call volume | More approval creation requests flowing through agent-module. | Monitor approval creation latency. Agent-module delegation to `ApprovalPort` is in-process and sub-millisecond, so this is unlikely to bottleneck. |

---

### R-SCALE-03 — External API Latency Increase

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| Saga duration | 2× API latency doubles saga wall-clock time. `AgentEventExecutionPort` calls are the primary external latency source. | Circuit breaker opens at configured slow-call threshold. Saga timeout caps total duration. |
| Thread pool saturation | Blocking calls via `AgentAvailabilityPort` and `AgentEventExecutionPort` hold threads longer. | Virtual threads (Java 21) mitigate. Monitor active thread count. |

---

### R-SCALE-04 — Multi-Region Deployment

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| Single PostgreSQL instance | Cross-region latency for every database call, including `CoordinationProtocolPort` state transitions. | Requires PostgreSQL with cross-region replication or regional database instances with conflict resolution. This is a fundamental architecture change. |
| Pessimistic locking | Locks acquired across regions have high latency. Both orchestrator and `CoordinationProtocolPort` paths affected. | Evaluate region-affinity routing: coordinations pinned to the region of the initiating agent. |
| Event bus | In-process events are inherently single-node. Agent-mediated flows (approval event → handler → `CoordinationProtocolPort`) cannot cross region boundaries. | Requires migration to a durable event transport (outbox + CDC or broker). |

Multi-region deployment is not supported by the current architecture. It requires deliberate re-architecture and is out of scope for the initial release.

---

### R-SCALE-05 — Agent Module Extraction to Microservice

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| In-process event bus | Events between coordination and agent modules must cross a network boundary. Agent-module's event handlers (`CollaborativeApprovalDecisionHandler`, `CollaborativeApprovalExpiredHandler`) that call `CoordinationProtocolPort` must be converted to consume events from a durable transport. | Replace in-process events with outbox-based messaging or synchronous API calls. |
| `CoordinationProtocolPort` becomes a network call | Agent-module's `CoordinationProtocolPort.advance()` and `terminate()` calls currently execute in-process. After extraction, they become HTTP/gRPC calls to the coordination service. Latency increases from sub-millisecond to network round-trip time. Pessimistic lock is held by the coordination service, not the calling agent service. | Expose `CoordinationProtocolPort` as a REST/gRPC endpoint in coordination-service. Agent-service calls it synchronously. Timeout and retry configured on the client. |
| `AgentApprovalPort` becomes a network call | Coordination-module's `AgentApprovalPort` calls (approval creation) become outbound HTTP calls to the agent service. | Agent-service exposes an approval-creation endpoint. Coordination-service calls it via HTTP client adapter implementing `AgentApprovalPort`. |
| Shared database | Agent tables must be migrated to a separate database. | No cross-module JOINs exist (enforced by ArchUnit). Migration is schema-only; no query refactoring needed. |
| Transaction boundary | Coordination and agent operations can no longer share a database transaction. | Introduce explicit API contracts (REST or gRPC) at the module boundary. Saga pattern already handles cross-boundary consistency for calendar operations. Approval creation via `AgentApprovalPort` must become an idempotent API call with retry semantics. |

Hexagonal module boundaries, formalized port interfaces (`CoordinationProtocolPort`, `AgentApprovalPort`, `AgentAvailabilityPort`, `AgentEventExecutionPort`, `AgentProfilePort`), and the no-cross-module-JOIN constraint (ADR-08) were designed specifically to make this extraction feasible.

---

### R-SCALE-06 — Multiple Calendar Providers

| Pressure Point | Effect | Mitigation Threshold |
|----------------|--------|----------------------|
| `CalendarPort` abstraction | Current implementation assumes Google Calendar. Additional providers require new adapter implementations behind the same port. Agent-module selects the appropriate adapter based on user configuration. All coordination-module interactions via `AgentAvailabilityPort` and `AgentEventExecutionPort` remain unchanged. | Port interface is provider-agnostic. New adapters implement the same contract. |
| Saga compensation | Different providers have different API semantics for event deletion. | Compensation logic must be adapter-specific. Saga orchestrator remains unchanged — it calls `AgentEventExecutionPort` which delegates to the agent, which selects the correct `CalendarPort` adapter. |
| Token management | Each provider has a different OAuth flow and token format. | Token encryption (AES-256-GCM) is provider-agnostic. Token storage schema must include a provider discriminator column. |

---

## 6. Security & Compliance Risks

### R-SEC-01 — OAuth Token Exposure

| Attribute | Detail |
|-----------|--------|
| Description | Encrypted OAuth tokens stored in PostgreSQL. If database backup is exfiltrated and AES key is compromised, all tokens are exposed. |
| Severity | Critical |
| Likelihood | Rare |
| Risk Score | 5 |
| Mitigation | AES-256-GCM encryption at rest. Key stored in external secret manager, not in database. Database backups encrypted at infrastructure level. Access to production database restricted to service account only. Agent sovereignty ensures tokens are decrypted only within agent-module's `CalendarPort` adapter — coordination-module never handles tokens. |

---

### R-SEC-02 — AES Key Leakage

| Attribute | Detail |
|-----------|--------|
| Description | AES encryption key exposed via misconfigured environment variable, log output, or container image layer. |
| Severity | Critical |
| Likelihood | Unlikely |
| Risk Score | 10 |
| Mitigation | Key injected at runtime from secret manager. Never written to disk, logs, or configuration files. Application startup validates key presence and length. Log scrubbing rules prevent accidental key logging. See also TD-04 for automated rotation. |

---

### R-SEC-03 — Incomplete GDPR Deletion

| Attribute | Detail |
|-----------|--------|
| Description | GDPR data deletion request must remove all PII. Edge cases: PII embedded in coordination history, approval records (created via `AgentApprovalPort`), log entries, or cached data. The `trigger_source` field in `coordination_state_log` (QS-A1) references agent operations by user-scoped agent ID, which must be anonymized or deleted. |
| Severity | High |
| Likelihood | Possible |
| Risk Score | 12 |
| Mitigation | PII fields identified and tagged in schema. Deletion procedure covers agent table, coordination participant references (anonymized, not deleted, to preserve audit integrity), approval records created via `AgentApprovalPort`, and cache eviction. Log entries use correlation IDs, not PII. The `trigger_source` field references internal agent IDs, not user PII. GDPR deletion is verified by automated integration test that asserts no PII remains after procedure execution across all module-owned tables. |

---

### R-SEC-04 — Correlation ID Leaking PII

| Attribute | Detail |
|-----------|--------|
| Description | If correlation IDs are derived from or contain user identifiers, log aggregation systems expose PII to operators. |
| Severity | Medium |
| Likelihood | Unlikely |
| Risk Score | 6 |
| Mitigation | Correlation IDs are UUIDs generated at request ingress. No PII component. Agent IDs in logs are internal surrogate keys, not email addresses or names. The `trigger_source` field in audit logs uses generic labels ("agent-via-protocol-port", "orchestrator"), not user-identifiable values. |

---

### R-SEC-05 — Log Retention Misconfiguration

| Attribute | Detail |
|-----------|--------|
| Description | Logs retained beyond GDPR-required period or not retained long enough for audit requirements. |
| Severity | Medium |
| Likelihood | Possible |
| Risk Score | 9 |
| Mitigation | Log retention policy defined: 90 days for application logs, 1 year for audit logs (including `coordination_state_log` with `trigger_source` entries). Infrastructure-level lifecycle policies enforce deletion. Audit log entries stored in a separate log stream with distinct retention. |

---

### R-SEC-06 — JWT Secret Compromise

| Attribute | Detail |
|-----------|--------|
| Description | JWT signing secret exposed. Attacker can forge authentication tokens and impersonate any user. |
| Severity | Critical |
| Likelihood | Unlikely |
| Risk Score | 10 |
| Mitigation | Secret stored in external secret manager. Rotation procedure documented. Short-lived token (24 hours) limits exposure window. Secret rotation invalidates all tokens immediately. |

---

### R-SEC-07 — Clock Skew Affecting Signature Validation

| Attribute | Detail |
|-----------|--------|
| Description | Slack request signature validation and JWT expiry checks depend on accurate system time. Clock skew between nodes or relative to Slack servers causes false rejections or acceptances. Also affects approval timeout processing — clock skew between the node that created the approval (with expiry timestamp) and the scheduler node that checks expiration could cause premature or delayed expiration, which then triggers `CoordinationProtocolPort.terminate()` at the wrong time. |
| Severity | Medium |
| Likelihood | Unlikely |
| Risk Score | 6 |
| Mitigation | All nodes use NTP synchronization. Slack signature validation includes a 5-minute tolerance window (Slack's documented recommendation). JWT validation includes a 30-second clock skew allowance. Approval timeout tolerance of ±60 seconds (QS-P5) absorbs minor clock differences between creation and expiration nodes. |

---

### R-SEC-08 — Agent Sovereignty Bypass via Direct Module Access

| Attribute | Detail |
|-----------|--------|
| Description | A developer might inadvertently bypass the agent sovereignty boundary by importing approval-module or calendar-module directly from coordination-module, circumventing the mediation provided by `AgentApprovalPort` (ADR-14), `AgentAvailabilityPort`, or `AgentEventExecutionPort`. This would violate data isolation, credential containment, and extraction readiness guarantees. |
| Severity | High |
| Likelihood | Unlikely — ArchUnit rules prevent this at compile time. |
| Risk Score | 8 |
| Mitigation | ArchUnit fitness functions (QS-M2) enforce zero compile-time imports from coordination-module to calendar-module, approval-module, messaging-module, or user-module. Maven module POM has no dependency declarations for these modules. Build fails immediately on violation. Code review checklist includes agent sovereignty verification for any new port or adapter. |

---

## 7. Risk Mitigation Strategy

### 7.1 Preventive Controls

Preventive controls eliminate risk triggers before they manifest.

| Control | Target Risks | Implementation |
|---------|-------------|----------------|
| ArchUnit enforcement | R-ARCH-05, R-ARCH-08, R-ARCH-09, R-ARCH-10, R-SCALE-05, R-SEC-08 | Compile-time module boundary validation. No cross-module JOINs. No coordination→`CalendarPort` dependency. No coordination→`ApprovalPort` dependency. No coordination→`MessagingPort` dependency. Zero imports across agent sovereignty boundary except through defined ports (`AgentAvailabilityPort`, `AgentEventExecutionPort`, `AgentApprovalPort`, `AgentProfilePort`, `CoordinationProtocolPort`). |
| Property-based state machine tests | R-ARCH-05, R-ARCH-09 | Exhaustive transition matrix tested on every build. Invalid transitions provably rejected. Both orchestrator-driven and `CoordinationProtocolPort`-driven paths verified against the same transition matrix. |
| Schema migration validation | R-OPS-07 | Flyway migrations run in CI. No manual DDL. |
| Secret manager integration | R-SEC-01, R-SEC-02, R-SEC-06 | Secrets never in code, config files, or container images. |
| Idempotency keys | R-OPS-03 | Slack interaction deduplication prevents retry-induced duplication. |
| Agent handler unit tests | R-ARCH-09, R-OPS-08 | `CollaborativeApprovalDecisionHandler` and `CollaborativeApprovalExpiredHandler` tested with mocked `CoordinationProtocolPort`, verifying correct advance/terminate calls for all approval states. |

---

### 7.2 Detective Controls

Detective controls identify risks that have materialized so they can be contained.

| Control | Target Risks | Implementation |
|---------|-------------|----------------|
| Circuit breaker state metrics | R-ARCH-03, R-OPS-02 | Micrometer counters for OPEN/HALF_OPEN transitions. Alert on sustained OPEN state. |
| Connection pool utilization metrics | R-OPS-04, R-SCALE-01 | HikariCP metrics exported. Alert at 80% active connections. |
| Compensation failure counter | R-ARCH-01, TD-09 | Metric incremented on saga compensation failure. Alert at > 0 per hour. |
| Log anomaly detection | R-OPS-05 | Log volume baseline established. Alert on 3× deviation. |
| GDPR deletion verification test | R-SEC-03 | Automated test in CI that executes deletion procedure and asserts no PII residue. |
| `CoordinationProtocolPort` call metrics | R-ARCH-04, R-ARCH-09, R-OPS-08 | Micrometer timer and counter for advance/terminate operations (QS-O5). Alert on error rate > 1% or no-op rate > 5%. |
| Stuck coordination detector | R-OPS-08 | Reconciliation scheduler detects coordinations in awaiting-approval states beyond 2× approval timeout. Alert on `REQUIRES_MANUAL_INTERVENTION` transition count > 0 per day. |

---

### 7.3 Compensating Controls

Compensating controls limit damage after a risk has materialized.

| Control | Target Risks | Implementation |
|---------|-------------|----------------|
| Saga compensation | R-ARCH-03 | Automatic rollback of partial calendar event creation via `AgentEventExecutionPort.deleteEvent()`. |
| Scheduler re-evaluation | R-ARCH-01, R-OPS-08 | On restart, scheduler re-scans pending states. Implicitly retries missed event-driven side effects. Detects coordinations where `CoordinationProtocolPort` advancement was missed due to handler failure. |
| JWT secret rotation | R-SEC-06 | Documented procedure to rotate secret and invalidate all active tokens within 15 minutes (token expiry window). |
| Manual reconciliation runbook | TD-09 | Operator procedure to identify and delete orphaned calendar events when automated compensation via `AgentEventExecutionPort` fails. |

---

### 7.4 Refactoring Roadmap

| Phase | Trigger Threshold | Action |
|-------|------------------|--------|
| Phase 1 (Post-MVP) | User count > 1,000 or compensation failure rate > 0.1% | Implement transactional outbox (TD-01) — critical for reliable delivery of domain events consumed by agent-module handlers. Automate key rotation (TD-04). Introduce `CoordinationProtocolPort` command objects (TD-10) if method signatures have expanded. |
| Phase 2 (Growth) | Coordination volume > 100/min or P99 saga duration > 10s | Parallelize saga steps via concurrent `AgentEventExecutionPort` calls. Introduce virtual-thread-based execution. Externalize scheduler intervals (TD-08). Extract agent-mediation domain service (TD-11) if agent-module complexity warrants. |
| Phase 3 (Scale) | Need for per-module scaling or multi-region | Extract first module (agent). Convert `CoordinationProtocolPort` to REST endpoint. Convert `AgentApprovalPort` to REST client. Introduce distributed event transport. Evaluate regional database topology. |

---

### 7.5 Architecture Evolution Decision Thresholds

The following metrics, when sustained over a 7-day window, trigger an architecture review with explicit decision to evolve or accept:

| Metric | Threshold | Decision |
|--------|-----------|----------|
| DB connection pool utilization P95 | > 80% | Evaluate read replicas or connection pooler (PgBouncer). |
| Saga duration P99 | > 8 seconds | Evaluate parallel saga steps via concurrent `AgentEventExecutionPort` calls. |
| LLM fallback ratio | > 30% of intents | Invest in rule-based parser coverage. |
| Compensation failure rate | > 0.5% | Implement automated reconciliation (TD-09). |
| Cache hit ratio | < 50% | Evaluate shared cache layer. |
| `CoordinationProtocolPort` error rate | > 1% | Investigate root cause. Evaluate circuit breaker on protocol port calls if external factors are involved post-extraction. |
| `CoordinationProtocolPort` no-op rate | > 5% | Investigate duplicate event delivery or stale approval events. May indicate need for transactional outbox (TD-01). |
| Agent-module class count | > 30 classes | Evaluate sub-module splitting per R-ARCH-10 future mitigation. |

---

## 8. Risk Monitoring & Early Warning Indicators

| Indicator | Metric Name | Warning Threshold | Critical Threshold | Related Risk |
|-----------|-------------|-------------------|-------------------|-------------|
| Circuit breaker open ratio | `resilience4j.circuitbreaker.state{state="open"}` | > 5% of 5-min window | > 20% of 5-min window | R-ARCH-03, R-OPS-02 |
| Compensation failure rate | `coagent.saga.compensation.failure.total` | > 0 per hour | > 5 per hour | R-ARCH-01, TD-09 |
| LLM fallback ratio | `coagent.intent.llm_fallback.ratio` | > 25% | > 40% | TD-06 |
| DB connection utilization | `hikaricp.connections.active / hikaricp.connections.max` | > 70% | > 90% | R-OPS-04, R-SCALE-01 |
| Approval timeout backlog | `coagent.scheduler.pending_expirations.count` | > 100 | > 500 | R-OPS-06 |
| Saga reconciliation retries | `coagent.saga.retry.total` | > 10 per hour | > 50 per hour | R-ARCH-03 |
| Coordination duration P99 | `coagent.coordination.duration.p99` | > 5 seconds | > 10 seconds | R-ARCH-03, R-ARCH-04 |
| Lock wait time P99 | `coagent.db.lock_wait.duration.p99` | > 500 ms | > 2 seconds | R-ARCH-04 |
| Event bus listener error rate | `coagent.events.listener.error.total` | > 0 per minute | > 10 per minute | R-ARCH-01, R-OPS-08 |
| Slack webhook response time P95 | `coagent.slack.webhook.duration.p95` | > 2 seconds | > 3 seconds | R-OPS-03 |
| Protocol port call latency P99 | `coagent.coordination.protocol.port.duration.p99` | > 200 ms | > 1 second | R-ARCH-04, R-ARCH-09 |
| Protocol port error rate | `coagent.coordination.protocol.port.error.rate` | > 1% | > 5% | R-ARCH-09, R-OPS-08 |
| Protocol port no-op rate | `coagent.coordination.protocol.port.noop.rate` | > 5% | > 15% | R-ARCH-09 |
| Stuck coordination count | `coagent.coordination.stuck.awaiting_approval.count` | > 0 for > 24 hours | > 5 for > 24 hours | R-OPS-08 |

All indicators are exported via Micrometer to the configured metrics backend (Prometheus-compatible). Alerting rules are defined in the deployment infrastructure (Grafana Alerting or equivalent) and reference the thresholds above.

---

## 9. Risk Review Cadence

### Monthly Architectural Risk Review

**Participants:** Tech Lead, Senior Engineers, Product Owner (optional).

**Scope:** Review all risks scored ≥ 12. Update likelihood and impact based on production data. Reprioritize technical debt items based on observed indicators. Specifically review `CoordinationProtocolPort` metrics (no-op rate, error rate, latency) and agent-module complexity metrics (class count, handler count) against thresholds. Duration: 60 minutes.

**Inputs:** Metrics dashboard (Section 8 indicators, including new protocol port and stuck coordination indicators), incident reports from the past month, technical debt backlog.

**Outputs:** Updated risk register, new or re-prioritized debt items, architecture decision proposals if thresholds are breached.

---

### Quarterly Scalability Review

**Participants:** Architect, Tech Lead, SRE/DevOps.

**Scope:** Review scaling indicators against growth projections. Evaluate whether Phase 1/2/3 refactoring triggers (Section 7.4) are approaching. Assess connection pool sizing, cache effectiveness, saga throughput, and `CoordinationProtocolPort` call volume against projected load. Evaluate agent-module growth trajectory against R-ARCH-10 thresholds.

**Inputs:** Traffic growth trends, infrastructure cost reports, P99 latency trends, `CoordinationProtocolPort` call volume trends.

**Outputs:** Capacity plan for next quarter, infrastructure change proposals, architecture evolution decisions.

---

### Pre-Release Security Audit

**Trigger:** Every production release that modifies authentication, authorization, token handling, encryption paths, or agent sovereignty boundary (`AgentApprovalPort`, `CoordinationProtocolPort`, or any new agent capability port).

**Scope:** Review Slack signature validation, JWT configuration, AES key handling, GDPR deletion procedure. Verify no secrets in container image layers or configuration files. Verify ArchUnit agent sovereignty rules pass. Run automated GDPR deletion verification test. Verify that new ports do not create unauthorized cross-module data access paths.

**Outputs:** Security sign-off or blocking issues list.

---

### Post-Incident Architectural Review

**Trigger:** Any production incident with severity Critical or High, any incident involving data integrity or compensation failure, or any incident where a `CoordinationProtocolPort` call failed to advance or terminate a coordination.

**Timing:** Within 5 business days of incident resolution.

**Scope:** Root cause analysis mapped to risk register. Determine if the incident was a known risk (update likelihood) or a new risk (add to register). Evaluate whether existing mitigations were effective. For protocol port incidents, verify that the `trigger_source` field in `coordination_state_log` correctly recorded the failed transition attempt. Propose preventive or detective control improvements.

**Outputs:** Updated risk register entry, new ADR if architectural change is warranted, updated runbooks if operational procedure was insufficient.

---

*End of Risks and Technical Debt Documentation*
