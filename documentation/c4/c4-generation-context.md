# CoAgent4U — C4 Diagram Generation Context

> **Authoritative Architectural Contract**
> Derived from: PRD v1.0 (Feb 14, 2026) · arc42 Documentation · ADR-01 through ADR-14

---

## 1. Architectural Authority

The following documents are the single source of truth:

1. PRD v1.0 (Feb 14, 2026)
2. arc42 documentation (sections 01–12)
3. ADR decisions (ADR-01 through ADR-14)
4. Deterministic Coordination State Machine
5. MVP Constraints

C4 diagrams **MUST NOT** introduce architecture beyond these documents.

- If any ambiguity exists, **PRD overrides arc42**.
- If any conflict exists, **ADR overrides implementation assumptions**.

---

## 2. System Identity (Non-Negotiable)

| Property | Value |
|---|---|
| **System Name** | CoAgent4U |
| **System Type** | Deterministic Personal Agent Coordination Platform |

**Core Characteristics:**
- Human-in-the-Loop (HITL) governance
- Structured Agent-to-Agent (A2A) coordination
- Deterministic state-machine driven protocol
- Modular Monolith architecture
- Hexagonal Architecture (Ports & Adapters)
- GDPR-aligned system design

**It is NOT:**
- A chatbot
- An AI orchestration engine
- An automation script
- An AI negotiation system
- A microservices architecture

---

## 3. MVP Constraints (Strict)

MVP supports:
- Two-user coordination only
- Google Calendar only
- Single timezone only
- No recurring meetings
- No multi-party scheduling
- No admin roles
- No AI-based negotiation logic
- No business-hours logic
- No timezone conversions
- No external message broker
- No event streaming infrastructure

> C4 diagrams **MUST NOT** violate these constraints.

---

## 4. Deployment Model

| Property | Value |
|---|---|
| **Architecture Style** | Modular Monolith (ADR-01) |
| **Deployable Unit** | Single Spring Boot Application |
| **Database** | Single PostgreSQL database |
| **Application Nodes** | Stateless |

**No:**
- Microservices
- Distributed services
- Kafka / RabbitMQ
- Workflow engines
- External event buses

---

## 5. Core Domain Modules

Modules defined in arc42:

| # | Module |
|---|---|
| 1 | UserModule |
| 2 | AgentModule |
| 3 | CoordinationModule |
| 4 | ApprovalModule |
| 5 | IntegrationCalendarModule |
| 6 | IntegrationMessagingModule |
| 7 | IntegrationLLMModule |
| 8 | InfrastructurePersistence |
| 9 | InfrastructureSecurity |
| 10 | InfrastructureConfig |
| 11 | InfrastructureMonitoring |

Each module:
- Owns its tables
- Has an isolated domain
- Communicates only through ports
- Must respect Agent Sovereignty

---

## 6. Agent Sovereignty Principle (ADR-03)

**Rules:**
- `CoordinationModule` **NEVER** calls `CalendarPort` directly.
- `CoordinationModule` **NEVER** calls Approval persistence directly.
- All user-scoped operations are mediated through `AgentModule`.

**Authorized Ports from CoordinationModule → AgentModule:**
- `AgentAvailabilityPort`
- `AgentEventExecutionPort`
- `AgentApprovalPort`
- `AgentProfilePort`

- Coordination state advancement occurs **ONLY** via `CoordinationProtocolPort`.
- Approval creation occurs **ONLY** via `AgentApprovalPort`.
- Calendar mutation authority exists **ONLY** inside `AgentModule`.

---

## 7. Deterministic Coordination State Machine

### Valid State Sequence

```
INITIATED
→ CHECKING_AVAILABILITY_A
→ CHECKING_AVAILABILITY_B
→ MATCHING
→ PROPOSAL_GENERATED
→ AWAITING_APPROVAL_B
→ APPROVED_BY_B
→ AWAITING_APPROVAL_A
→ APPROVED_BY_BOTH
→ COMPLETED
```

**Terminal States:** `FAILED` · `REJECTED` · `COMPLETED`

**Rules:**
- No implicit transitions.
- No skipped states.
- No AI-based decision logic.
- Invitee approves first.
- Single rejection terminates coordination.
- Approval timeout: **12 hours**.
- Calendar events created **ONLY** after `APPROVED_BY_BOTH`.

---

## 8. LLM Usage Constraint (ADR-10)

**LLM is allowed ONLY for:**
- Intent fallback parsing
- Text summarization

**LLM is NOT allowed to:**
- Influence matching logic
- Select proposals
- Advance state machine
- Override deterministic rules
- Perform negotiation

---

## 9. Hexagonal Architecture Rules (ADR-02)

| Layer | Rules |
|---|---|
| **Domain Layer** | Zero framework dependencies · Pure business logic · State machine enforcement |
| **Application Layer** | Defines Ports · Orchestrates use cases · Calls outbound ports |
| **Adapters Layer** | Implements ports · Infrastructure logic only · No business logic allowed |
| **Infrastructure Layer** | Spring · Security · Persistence · Configuration · Monitoring |

> C4 diagrams **MUST** reflect this separation logically.

---

## 10. External Systems

| System | Role |
|---|---|
| **Slack Platform** | Entry point for commands (webhook) |
| **Google Calendar API** | Accessed ONLY via `AgentModule` |
| **LLM API** (Groq or equivalent) | Accessed ONLY via `IntegrationLLMModule`; fallback parsing only |
| **Web Dashboard** | User-facing UI |

---

## 11. Container Model (Level 2)

**Inside System Boundary:**

| Container | Description |
|---|---|
| **CoAgent4U Application** | Spring Boot Modular Monolith — contains all modules |
| **PostgreSQL Database** | Single database instance |

> No additional containers are allowed.

---

## 12. Component Naming (Level 3)

### CoordinationModule
- `CoordinationOrchestrator`
- `CoordinationSaga`
- `CoordinationStateMachine`
- `CoordinationProtocolPort`
- `CoordinationRepositoryPort`

### AgentModule
- `AgentAvailabilityPort`
- `AgentEventExecutionPort`
- `AgentApprovalPort`
- `AgentProfilePort`
- `CalendarPort` *(internal only)*

### ApprovalModule
- `ApprovalAggregate`
- `ApprovalRepositoryPort`
- `ApprovalTimeoutScheduler`

### UserModule
- `UserAggregate`
- `UserRepositoryPort`

### IntegrationCalendarModule
- `GoogleCalendarAdapter`

### IntegrationMessagingModule
- `SlackInboundAdapter`
- `SlackOutboundAdapter`

### IntegrationLLMModule
- `LLMParsingAdapter`

### InfrastructurePersistence
- `PostgreSQLAdapter`
- `FlywayMigration`

### InfrastructureSecurity
- `JwtAuthenticationFilter`
- `SlackSignatureVerifier`
- `EncryptionAdapter` (AES-256)

### InfrastructureMonitoring
- `MetricsCollector`
- `AuditLogger`

---

## 13. Prohibited in C4 Diagrams

**DO NOT introduce:**
- Microservices
- Kafka / RabbitMQ
- Event streaming systems
- Async workflow engines
- AI negotiation engine
- Multi-party scheduling
- Timezone abstraction layer
- Admin service
- Separate coordination service
- Direct Coordination → Calendar calls

---

## 14. Output Format Constraint

When generating diagrams:
- Use either **Mermaid C4** or **Structurizr DSL**.
- Do not include speculative future features.
- Do not extend beyond MVP.
- Use exact naming defined above.
- Maintain deterministic architecture.
- Preserve Agent Sovereignty.
- Reflect modular monolith deployment model.
