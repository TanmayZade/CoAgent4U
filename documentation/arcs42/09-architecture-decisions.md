# 9. Architecture Decisions

## Table of Contents

- [9.1 Decision Record Format](#91-decision-record-format)
- [9.2 Summary of Key Decisions](#92-summary-of-key-decisions)
- [9.3 ADR-01 through ADR-14](#93-adr-01-through-adr-14)
  - [ADR-01 — Modular Monolith over Microservices](#adr-01--modular-monolith-over-microservices)
  - [ADR-02 — Hexagonal Architecture with Strict Port Isolation](#adr-02--hexagonal-architecture-with-strict-port-isolation)
  - [ADR-03 — Agent Sovereignty Principle](#adr-03--agent-sovereignty-principle)
  - [ADR-04 — Deterministic Coordination State Machine](#adr-04--deterministic-coordination-state-machine)
  - [ADR-05 — Saga with Compensating Transactions](#adr-05--saga-with-compensating-transactions)
  - [ADR-06 — In-Process Domain Event Bus](#adr-06--in-process-domain-event-bus)
  - [ADR-07 — Pessimistic Locking over Optimistic Locking](#adr-07--pessimistic-locking-over-optimistic-locking)
  - [ADR-08 — Single PostgreSQL with Table Ownership](#adr-08--single-postgresql-with-table-ownership)
  - [ADR-09 — Stateless Application Nodes](#adr-09--stateless-application-nodes)
  - [ADR-10 — Two-Tier Intent Parsing with LLM Fallback](#adr-10--two-tier-intent-parsing-with-llm-fallback)
  - [ADR-11 — 12-Hour Approval Timeout with Scheduled Batch](#adr-11--12-hour-approval-timeout-with-scheduled-batch)
  - [ADR-12 — Cloud-Agnostic Containerized Deployment](#adr-12--cloud-agnostic-containerized-deployment)
  - [ADR-13 — CoordinationProtocolPort](#adr-13--coordinationprotocolport)
  - [ADR-14 — AgentApprovalPort](#adr-14--agentapprovalport)
- [9.4 Decision Impact Summary](#94-decision-impact-summary)

---

## 9.1 Decision Record Format

Each ADR contains: Metadata (status, date, affects), Context, Decision, Alternatives Considered, Consequences.

Status values: `Proposed` | `Accepted` | `Superseded` | `Deprecated`

---

## 9.2 Summary of Key Decisions

| ADR | Title | Status | Primary Driver | Affected Areas |
|-----|-------|--------|----------------|----------------|
| ADR-01 | Modular Monolith over Microservices | Accepted | OC-01, OC-03 | Deployment, module structure, persistence |
| ADR-02 | Hexagonal Architecture with Strict Port Isolation | Accepted | Q7, Q6 | All modules, build config |
| ADR-03 | Agent Sovereignty Principle | Accepted | Q6, extraction readiness | coordination, agent, integration modules |
| ADR-04 | Deterministic Coordination State Machine | Accepted | Q1, Q3 | coordination-module domain layer |
| ADR-05 | Saga with Compensating Transactions | Accepted | Q5 | coordination-module, agent-module |
| ADR-06 | In-Process Domain Event Bus | Accepted | OC-01, OC-03 | Cross-module communication |
| ADR-07 | Pessimistic Locking over Optimistic Locking | Accepted | Q1, concurrency | Persistence, coordination, approval |
| ADR-08 | Single PostgreSQL with Table Ownership | Accepted | OC-03, Q6 | Persistence, all core modules |
| ADR-09 | Stateless Application Nodes | Accepted | Horizontal scaling | Deployment, caching |
| ADR-10 | Two-Tier Intent Parsing with LLM Fallback | Accepted | Q1, OC-03 | agent-module, llm-module |
| ADR-11 | 12-Hour Approval Timeout with Scheduled Batch | Accepted | PRD mandate, Q1 | approval-module, scheduling |
| ADR-12 | Cloud-Agnostic Containerized Deployment | Accepted | OC-04, OC-03 | Infrastructure, CI/CD |
| ADR-13 | CoordinationProtocolPort for Agent-Mediated Advancement | Accepted | Q6, ADR-03 | coordination-module, agent-module |
| ADR-14 | AgentApprovalPort for Agent-Mediated Approval Creation | Accepted | Q6, ADR-03 | coordination, agent, approval modules |

---

## 9.3 ADR-01 through ADR-14

### ADR-01 — Modular Monolith over Microservices

- **Context:** Solo developer (OC-01), minimal budget (OC-03), can't sustain microservices overhead
- **Decision:** Single Spring Boot app, internally decomposed into bounded Maven modules with ArchUnit enforcement
- **Rejected:** Microservices (too complex for solo dev), plain monolith (no boundaries), serverless (stateful workflows don't fit)
- **Consequences:** Simple CI/CD and debugging; shared JVM resource pool; extraction-ready module boundaries

---

### ADR-02 — Hexagonal Architecture with Strict Port Isolation

- **Context:** Three external integrations (Slack, Google Calendar, Groq); domain must be testable in isolation (Q7)
- **Decision:** Three layers per module — domain (zero framework deps), application (ports + use cases), adapters (implementations)
- **Rejected:** Layered architecture (JPA leaks into domain), vertical slices (obscures shared coordination model)
- **Consequences:** ~20 port interfaces; domain tests run without Spring; adapter replacement is localized

---

### ADR-03 — Agent Sovereignty Principle

- **Context:** Coordination-module had transitive deps on calendar, approval, messaging, and user modules
- **Decision:** All user-specific operations mediated by agents via explicit ports:
  - `AgentAvailabilityPort` — coordination → agent (availability checks)
  - `AgentEventExecutionPort` — coordination → agent (event create/delete)
  - `AgentProfilePort` — coordination → agent (user metadata)
  - `AgentApprovalPort` — coordination → agent (approval creation, see ADR-14)
  - `CoordinationProtocolPort` — agent → coordination (state advancement, see ADR-13)
- **Rejected:** Direct port access from coordination, shared services, async-only negotiation
- **Consequences:** Clean extraction boundaries; agent owns all user-scoped integrations; 5 agent-facing ports

---

### ADR-04 — Deterministic Coordination State Machine

- **Context:** Multi-step async workflow needs determinism (Q1) and full auditability (Q3)
- **Decision:** Explicit FSM with named states (`INITIATED → CHECKING_AVAILABILITY_A → … → COMPLETED/FAILED/REJECTED`). Two entry paths:
  - `CoordinationOrchestrator` — drives synchronous phases
  - `CoordinationProtocolPort` — agents advance state for approval decisions
  - Both use same `CoordinationStateMachine` domain service + pessimistic locking
- **Rejected:** Event choreography (non-deterministic), BPMN engine (overkill), LLM negotiation (non-deterministic)
- **Consequences:** Exhaustively testable transitions; append-only `coordination_state_log`; sequential execution adds latency

---

### ADR-05 — Saga with Compensating Transactions

- **Context:** Dual calendar event creation requires "both or neither" — Google Calendar has no distributed tx support
- **Decision:** Two-step saga through agent ports:
  - Step 1: Agent A creates event → persist `eventId_A` → state `CREATING_EVENT_A`
  - Step 2: Agent B creates event → if fails, compensate by deleting Agent A's event
  - Reconciliation task every 5 min for stuck intermediate states (>2 min threshold)
- **Rejected:** Two-phase commit (not supported by Google), best-effort (unacceptable inconsistency)
- **Consequences:** Eventual consistency window during saga; compensation may fail (retry 3x then manual alert)

---

### ADR-06 — In-Process Domain Event Bus

- **Context:** Solo dev (OC-01) can't operate external broker; modules need decoupled communication
- **Decision:** Spring `ApplicationEventPublisher` for domain events; synchronous within transaction by default
- **Rejected:** RabbitMQ/Kafka (operational overhead), direct method calls only (tight coupling)
- **Consequences:** Zero infrastructure overhead; events lost on crash (acceptable for non-critical notifications); no replay capability

---

### ADR-07 — Pessimistic Locking over Optimistic Locking

- **Context:** Concurrent approval callbacks and timeout schedulers can race on same coordination row
- **Decision:** `SELECT FOR UPDATE` on coordination rows; `SKIP LOCKED` for batch operations
- **Rejected:** Optimistic locking (retry storms under contention), application-level mutex (doesn't survive restarts)
- **Consequences:** Eliminates race conditions; potential lock contention under high load (acceptable at MVP scale)

---

### ADR-08 — Single PostgreSQL with Table Ownership

- **Context:** Budget constraint (OC-03); need logical separation without multiple databases
- **Decision:** One PostgreSQL instance; each module owns its tables (prefix convention); no cross-module JOINs; module reads others' data only through ports
- **Rejected:** Database-per-module (operational cost), shared tables (no ownership boundaries)
- **Consequences:** Simple ops; schema coupling risk mitigated by port-only access rule; single point of failure

---

### ADR-09 — Stateless Application Nodes

- **Context:** Need horizontal scaling without sticky sessions or shared memory
- **Decision:** No in-memory session state; all state in PostgreSQL; JWT/token-based auth; any node can handle any request
- **Rejected:** Sticky sessions (scaling constraint), distributed cache (complexity)
- **Consequences:** Trivial horizontal scaling; every request hits DB (acceptable at MVP scale)

---

### ADR-10 — Two-Tier Intent Parsing with LLM Fallback

- **Context:** Slack messages need intent extraction; LLM calls are expensive (OC-03) and non-deterministic
- **Decision:** Tier 1: regex/keyword rules for common intents (schedule, approve, reject). Tier 2: LLM fallback via Groq for ambiguous messages. Deterministic path handles ~80% of inputs.
- **Rejected:** LLM-only (cost, non-determinism), rules-only (too rigid for natural language)
- **Consequences:** Predictable cost; deterministic for common cases; LLM fallback adds latency (~500ms)

---

### ADR-11 — 12-Hour Approval Timeout with Scheduled Batch

- **Context:** PRD mandates 12-hour approval window; need deterministic expiration
- **Decision:** Approvals have `expiresAt` timestamp; scheduled batch job runs every 15 min, expires overdue approvals, publishes `ApprovalExpired` event; agent handles event and calls `CoordinationProtocolPort.terminate()`
- **Rejected:** Real-time per-approval timers (resource waste at scale), no timeout (workflow stalls indefinitely)
- **Consequences:** Up to 15-min granularity on expiration; simple implementation; batch query is index-optimized

---

### ADR-12 — Cloud-Agnostic Containerized Deployment

- **Context:** Portability requirement (OC-04); must not lock into AWS/GCP/Azure
- **Decision:** Docker container; docker-compose for local/staging; Kubernetes-ready Helm charts; no cloud-specific services in critical path
- **Rejected:** Cloud-native PaaS (vendor lock-in), bare metal (operational burden)
- **Consequences:** Runs anywhere Docker runs; foregoes managed services (e.g., Cloud SQL, SQS); slightly more ops work

---

### ADR-13 — CoordinationProtocolPort

| Attribute | Value |
|-----------|-------|
| Status | Accepted |
| Date | 2026-02-18 |
| Affects | coordination-module (inbound port), agent-module (caller) |

- **Context:** Agents receive `ApprovalDecisionMade`/`ApprovalExpired` events but had no structured way to advance coordination state without the coordination-module directly consuming approval events (violating ADR-03)
- **Decision:** `CoordinationProtocolPort` is an inbound port in coordination-module:
  - `advance(coordinationId, decision, agentId)` → returns `CoordinationProgressionResult`
  - `terminate(coordinationId, reason, agentId)` → void
  - Called by agent-module handlers (`CollaborativeApprovalDecisionHandler`, `CollaborativeApprovalExpiredHandler`)
  - Uses pessimistic row lock (`SELECT FOR UPDATE`)
  - Audit logged with trigger source `"agent-via-protocol-port"`
- **Rejected:** Coordination directly consuming approval events (violates ADR-03), async event-only advancement (non-deterministic)
- **Consequences:** Clean agent→coordination contract; synchronous lock-protected advancement; extraction-ready as network API; ~50 LOC overhead

---

### ADR-14 — AgentApprovalPort

| Attribute | Value |
|-----------|-------|
| Status | Accepted |
| Date | 2026-02-18 |
| Affects | coordination-module (outbound port), agent-module (implementer), approval-module (internal delegation) |

- **Context:** Coordination-module was directly calling `ApprovalPort` to create approvals, creating a compile-time dependency on approval-module (violating ADR-03)
- **Decision:** `AgentApprovalPort` is an outbound port in coordination-module, implemented by agent-module:
  - `createApprovalRequest(coordinationId, proposalDetails, targetAgentId)` → returns `ApprovalRequestConfirmation`
  - Agent-module internally delegates to `ApprovalPort` (approval-module)
  - Coordination-module has zero compile-time dependency on approval-module
  - Called by `CoordinationOrchestrator` during `PROPOSAL_GENERATED → AWAITING_APPROVAL` transition
- **Rejected:** Direct `ApprovalPort` access from coordination (violates ADR-03), shared approval service (ambiguous ownership)
- **Consequences:** Approval lifecycle owned by agent-module; coordination dependency graph is coordination → agent only; ~30 LOC overhead

---

## 9.4 Decision Impact Summary

| ADR | Q1 Determinism | Q3 Auditability | Q5 Reliability | Q6 Modularity | Q7 Testability | OC-01 Solo Dev | OC-03 Budget | OC-04 Portability |
|-----|:--------------:|:---------------:|:--------------:|:-------------:|:--------------:|:--------------:|:------------:|:-----------------:|
| ADR-01 | | | | ● | | ● | ● | |
| ADR-02 | | | | ● | ● | | | |
| ADR-03 | | | | ● | | | | |
| ADR-04 | ● | ● | | ● | | | | |
| ADR-05 | | | ● | ● | | | | |
| ADR-06 | | | | | | ● | ● | |
| ADR-07 | ● | | ● | | | | | |
| ADR-08 | | | | ● | | | ● | |
| ADR-09 | | | | | | | | ● |
| ADR-10 | ● | | | | | | ● | |
| ADR-11 | ● | | ● | | | | | |
| ADR-12 | | | | | | | ● | ● |
| ADR-13 | ● | ● | | ● | | | | |
| ADR-14 | | | | ● | | | | |

● = Primary positive impact
