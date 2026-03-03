# 12. Glossary

## Table of Contents

- [A](#a)
  - [Agent Sovereignty](#agent-sovereignty)
  - [AgentAvailabilityPort](#agentavailabilityport)
  - [AgentEventExecutionPort](#agenteventexecutionport)
  - [AgentProfilePort](#agentprofileport)
  - [Approval (Aggregate)](#approval-aggregate)
  - [ApprovalDecisionMade](#approvaldecisionmade)
  - [ApprovalExpired](#approvalexpired)
  - [Approval Timeout](#approval-timeout)
  - [ArchUnit](#archunit)
  - [AvailabilityBlock](#availabilityblock)
  - [AvailabilityMatcher](#availabilitymatcher)
- [C](#c)
  - [Caffeine Cache](#caffeine-cache)
  - [Circuit Breaker](#circuit-breaker)
  - [Compensation (Saga)](#compensation-saga)
  - [Coordination (Aggregate)](#coordination-aggregate)
  - [CoordinationSaga](#coordinationsaga)
  - [CoordinationStateMachine](#coordinationstatemachine)
- [D](#d)
  - [Deterministic Zone](#deterministic-zone)
  - [Domain Event](#domain-event)
  - [Domain Event Publisher](#domain-event-publisher)
- [H](#h)
  - [Hexagonal Architecture](#hexagonal-architecture)
- [I](#i)
  - [In-Process Event Bus](#in-process-event-bus)
  - [IntentParser (Two-Tier Parsing)](#intentparser-two-tier-parsing)
- [L](#l)
  - [LLM Fallback](#llm-fallback)
- [M](#m)
  - [Modular Monolith](#modular-monolith)
- [P](#p)
  - [Pessimistic Locking](#pessimistic-locking)
  - [Port (Inbound / Outbound)](#port-inbound--outbound)
  - [ProposalGenerator](#proposalgenerator)
- [S](#s)
  - [Saga](#saga)
  - [SKIP LOCKED](#skip-locked)
  - [Slack Webhook Verification](#slack-webhook-verification)
  - [Stateless Node](#stateless-node)
- [T](#t)
  - [Table Ownership](#table-ownership)
  - [Transactional Boundary](#transactional-boundary)
- [V](#v)
  - [Value Object](#value-object)

---

## A

### Agent Sovereignty

Architectural principle stating that each agent's external integrations (calendar, preferences, tokens) are accessible only through that agent's own module boundary. The coordination module never accesses `CalendarPort` or agent-owned data directly. All cross-module interaction occurs through well-defined inbound ports or domain events. This principle preserves module autonomy and ensures that extracting the agent module to an independent service requires no changes to the coordination module.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-03)

---

### AgentAvailabilityPort

Outbound port in the agent module responsible for querying an agent's calendar provider to retrieve free/busy information for a given time range. Returns `AvailabilityBlock` value objects. Implemented by the Google Calendar adapter. The coordination module does not invoke this port directly; it requests availability through the agent module's inbound service API, in accordance with Agent Sovereignty.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts

---

### AgentEventExecutionPort

Outbound port in the agent module responsible for creating, updating, or deleting calendar events in an agent's external calendar provider. Invoked by the `CoordinationSaga` during the event creation and compensation phases. Each invocation targets a single agent's calendar, ensuring that the saga interacts with each agent's calendar independently.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts

---

### AgentProfilePort

Outbound port in the agent module responsible for retrieving and persisting agent profile data, including display name, timezone, scheduling preferences, and encrypted OAuth tokens. Backed by PostgreSQL. Read paths may be served from Caffeine cache with short TTL.

**Related Sections:** 05-building-block-view

---

### Approval (Aggregate)

Domain aggregate representing a single participant's pending decision on a proposed meeting time. Each coordination that reaches the `PENDING_APPROVAL` state produces one approval instance per participant. The approval aggregate tracks its own lifecycle: pending, accepted, declined, or expired. It is the authoritative source for whether a participant has responded and what their decision was.

**Related Sections:** 05-building-block-view, 06-runtime-view

---

### ApprovalDecisionMade

Domain event emitted when a participant submits an accept or decline decision on an approval. Consumed by the coordination module to evaluate whether all approvals for a coordination are complete and to trigger the appropriate state transition (to `CONFIRMED` or `DECLINED`).

**Related Sections:** 06-runtime-view, 08-cross-cutting-concepts

---

### ApprovalExpired

Domain event emitted when an approval's timeout elapses without a participant decision. Published by the timeout scheduler after detecting an expired approval row via `SKIP LOCKED` scan. Consumed by the coordination module to transition the coordination to the `EXPIRED` state and initiate any necessary compensation.

**Related Sections:** 06-runtime-view, 08-cross-cutting-concepts

---

### Approval Timeout

The maximum duration a participant is allowed to respond to an approval request before the system automatically expires it. Configured per coordination at creation time. Enforced by the timeout scheduler, which periodically scans for approvals whose deadline has passed. The timeout mechanism uses pessimistic locking with `SKIP LOCKED` to ensure exactly-once expiration across horizontally scaled nodes.

**Related Sections:** 06-runtime-view, 09-architecture-decisions (ADR-07)

---

### ArchUnit

Compile-time architecture testing library used in the CI pipeline to enforce structural rules. In CoAgent4U, ArchUnit tests verify module boundary isolation (no cross-module direct imports), hexagonal dependency direction (domain core has no dependency on adapters or infrastructure), table ownership constraints (no cross-module JOINs), and the Agent Sovereignty principle (coordination module cannot reference calendar port interfaces).

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-08)

---

### AvailabilityBlock

Value object representing a contiguous block of free or busy time on an agent's calendar. Includes start time, end time, and status (free or busy). Returned by `AgentAvailabilityPort` and consumed by the `AvailabilityMatcher` to compute overlapping free windows across multiple agents.

**Related Sections:** 05-building-block-view

---

### AvailabilityMatcher

Domain service in the coordination module responsible for computing the intersection of free time blocks across all participants in a coordination. Receives a collection of `AvailabilityBlock` sets (one per agent) and produces a ranked list of candidate time slots. Operates entirely within the deterministic zone; contains no external I/O.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts

---

## C

### Caffeine Cache

In-process, non-authoritative cache library used for read-heavy, non-critical data paths such as agent profile lookups and scheduling preferences. Each application node maintains an independent cache instance. Cache entries are governed by short TTLs (60–120 seconds). Coordination state is never cached. Cache is explicitly non-authoritative: all writes go to PostgreSQL, and stale reads are acceptable within the TTL window.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-10), 11-risks-and-technical-debt (R-ARCH-07)

---

### Circuit Breaker

Resilience pattern applied to all outbound adapter calls (Google Calendar API, Slack API, LLM provider). Implemented via Resilience4j. Transitions through `CLOSED`, `OPEN`, and `HALF_OPEN` states based on configurable failure rate and slow call thresholds. When `OPEN`, calls fail fast without reaching the external service, preventing cascade failures. State transitions emit Micrometer metrics for observability and alerting.

**Related Sections:** 08-cross-cutting-concepts, 11-risks-and-technical-debt (R-OPS-02)

---

### Compensation (Saga)

The rollback mechanism within the `CoordinationSaga`. When the saga successfully creates a calendar event for Agent A but fails to create the event for Agent B, compensation deletes the already-created event for Agent A. Each saga step defines a forward action and a corresponding compensating action. Compensation is invoked in reverse order of successful steps. If compensation itself fails, the failure is logged and flagged for manual or automated reconciliation.

**Related Sections:** 06-runtime-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-05)

---

### Coordination (Aggregate)

The central domain aggregate in the system. Represents a single meeting scheduling lifecycle from initial request through availability collection, proposal generation, participant approval, and calendar event creation. Owns its state via the `CoordinationStateMachine`. All state transitions are performed within a pessimistic-locked transactional boundary. The coordination aggregate is the consistency boundary for all scheduling decisions.

**Related Sections:** 05-building-block-view, 06-runtime-view

---

### CoordinationSaga

Orchestration component responsible for achieving atomicity across dual calendar event creation. When a coordination is confirmed, the saga creates events in each participant's calendar sequentially. If any step fails, previously completed steps are compensated (events deleted). The saga is a custom implementation; no external workflow engine is used. Saga state is persisted in the coordination aggregate to survive restarts.

**Related Sections:** 06-runtime-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-05), 11-risks-and-technical-debt (TD-03)

---

### CoordinationStateMachine

Deterministic finite state machine governing all valid state transitions for the coordination aggregate. States include (at minimum) `INITIATED`, `COLLECTING_AVAILABILITY`, `PROPOSING`, `PENDING_APPROVAL`, `CONFIRMED`, `DECLINED`, `EXPIRED`, and `FAILED`. Transitions are triggered by domain commands and events. Invalid transitions are rejected and provably so via property-based tests. The state machine is the single authoritative definition of the coordination lifecycle.

**Related Sections:** 05-building-block-view, 06-runtime-view, 09-architecture-decisions (ADR-04)

---

## D

### Deterministic Zone

The region of the application architecture where behavior is fully deterministic and reproducible given the same inputs. Includes the `CoordinationStateMachine`, `AvailabilityMatcher`, `ProposalGenerator`, and all domain logic. The deterministic zone has no dependency on external services or non-deterministic computation. The LLM-backed `IntentParser` sits outside this zone and is explicitly classified as non-deterministic.

**Related Sections:** 08-cross-cutting-concepts

---

### Domain Event

An immutable record of something that happened within the domain. In CoAgent4U, domain events (e.g., `ApprovalDecisionMade`, `ApprovalExpired`, `CoordinationConfirmed`) are published via the in-process Spring `ApplicationEventPublisher`. Events are consumed by listeners within the same JVM process. Events are not durable; they exist only in memory and are lost if the JVM crashes between the database commit and listener execution.

**Related Sections:** 08-cross-cutting-concepts, 11-risks-and-technical-debt (R-ARCH-01, TD-01)

---

### Domain Event Publisher

The mechanism by which domain events are dispatched to listeners. In CoAgent4U, this is Spring's `ApplicationEventPublisher`, operating in-process and synchronously (by default) within the same thread as the publishing transaction. No external message broker (Kafka, RabbitMQ) is used. This is an intentional architectural decision to minimize infrastructure complexity at the cost of durability.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-06)

---

## H

### Hexagonal Architecture

Architectural pattern (also known as Ports and Adapters) structuring each module into three layers: domain core (entities, value objects, domain services), ports (interfaces defining inbound and outbound interactions), and adapters (implementations of ports for specific technologies). In CoAgent4U, the domain core has no dependency on Spring, JPA, or any infrastructure library. All external interaction flows through ports. Dependency direction is strictly inward: adapters depend on ports, ports depend on domain core, domain core depends on nothing external.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-02)

---

## I

### In-Process Event Bus

The event transport mechanism in CoAgent4U. Domain events are published and consumed within the same JVM process via Spring `ApplicationEventPublisher`. No network transport, serialization, or external broker is involved. Provides low latency and zero infrastructure overhead. Does not provide durability, ordering guarantees across nodes, or at-least-once delivery.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-06), 11-risks-and-technical-debt (R-ARCH-01)

---

### IntentParser (Two-Tier Parsing)

Component responsible for extracting structured scheduling intent from natural language messages received via Slack. Implements a two-tier strategy: the first tier applies deterministic rule-based pattern matching (regex, keyword extraction) to handle common, well-structured requests. If the first tier cannot resolve the intent with sufficient confidence, the second tier invokes an LLM for natural language understanding. The LLM tier is explicitly non-deterministic and is wrapped with validation against the allowed intent enum.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts

---

## L

### LLM Fallback

The second tier of the `IntentParser`. Invoked only when rule-based parsing fails to extract a valid intent. Calls an external large language model API with the user message and a structured output schema. The response is validated against the allowed intent enum before being accepted. Non-deterministic by nature: the same input may produce different outputs across invocations. Mitigated by `temperature=0` configuration and output validation. Monitored via the `coagent.intent.llm_fallback.ratio` metric.

**Related Sections:** 08-cross-cutting-concepts, 11-risks-and-technical-debt (TD-06)

---

## M

### Modular Monolith

The top-level deployment architecture of CoAgent4U. All modules (coordination, agent, messaging, calendar integration) are compiled and deployed as a single Spring Boot application (single JAR, single process). Modules are isolated by package structure, enforced by ArchUnit, and interact through defined interfaces and domain events — not direct class references. This architecture preserves the option to extract modules into independent services without requiring distributed systems infrastructure at launch.

**Related Sections:** 05-building-block-view, 09-architecture-decisions (ADR-01)

---

## P

### Pessimistic Locking

Concurrency control strategy used for coordination and approval aggregate state transitions. Implemented via PostgreSQL `SELECT ... FOR UPDATE`. Ensures that only one transaction can modify a given coordination row at a time, preventing lost updates and race conditions between concurrent approval decisions or approval-vs-timeout races. Lock acquisition timeout is configured to prevent indefinite blocking.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-07), 11-risks-and-technical-debt (R-ARCH-04)

---

### Port (Inbound / Outbound)

Interface definition within the hexagonal architecture. An inbound port defines operations that the outside world can invoke on the module (e.g., a use case interface called by a REST controller or Slack webhook adapter). An outbound port defines operations that the domain core requires from external systems (e.g., `AgentAvailabilityPort`, `AgentEventExecutionPort`). Ports are plain Java interfaces with no framework annotations. Adapters implement outbound ports; controllers and event listeners invoke inbound ports.

**Related Sections:** 05-building-block-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-02)

---

### ProposalGenerator

Domain service responsible for selecting and ranking candidate meeting times from the output of the `AvailabilityMatcher`. Applies agent preferences (preferred time-of-day, meeting duration, buffer time) to score candidates. Operates within the deterministic zone. Produces a ranked list of proposals that are presented to participants for approval.

**Related Sections:** 05-building-block-view

---

## S

### Saga

An application-level pattern for managing consistency across multiple operations that cannot be wrapped in a single database transaction. In CoAgent4U, the saga manages dual calendar event creation: creating an event in Agent A's calendar and Agent B's calendar as a logical unit. Each step has a compensating action (event deletion). The saga is orchestrated (not choreographed), executed sequentially, and persisted as part of the coordination aggregate state. No external workflow engine is used.

**Related Sections:** 06-runtime-view, 08-cross-cutting-concepts, 09-architecture-decisions (ADR-05)

---

### SKIP LOCKED

PostgreSQL row-level locking modifier used in the timeout scheduler's polling query. `SELECT ... FOR UPDATE SKIP LOCKED` acquires locks on unclaimed rows and silently skips rows already locked by another transaction. This allows multiple horizontally scaled application nodes to run the same scheduler query concurrently without processing the same expired approval twice and without blocking each other.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-07)

---

### Slack Webhook Verification

Security mechanism ensuring that inbound HTTP requests to the Slack webhook endpoint originate from Slack's infrastructure. Implemented by validating the `X-Slack-Signature` header against an HMAC-SHA256 computed from the request body and the application's Slack signing secret. Requests failing verification are rejected with HTTP 401. A 5-minute timestamp tolerance prevents replay attacks while accommodating minor clock skew.

**Related Sections:** 08-cross-cutting-concepts

---

### Stateless Node

Deployment property of CoAgent4U application instances. No coordination state, session data, or user context is held in application memory between requests. All mutable state resides in PostgreSQL. Any request can be served by any node. This enables horizontal scaling by adding nodes behind a load balancer without session affinity. Caffeine caches hold non-authoritative read data only and can be lost without correctness impact.

**Related Sections:** 07-deployment-view, 09-architecture-decisions (ADR-09)

---

## T

### Table Ownership

Architectural constraint assigning each database table to exactly one module. Only the owning module may read from or write to its tables. No cross-module JOINs are permitted. Enforced by ArchUnit tests and code review. This constraint ensures that modules can be extracted to independent services with separate databases without requiring query refactoring.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-08)

---

### Transactional Boundary

The scope within which database operations are executed atomically. In CoAgent4U, each command handler (state transition, approval decision, saga step persistence) defines a transactional boundary that includes the aggregate write, domain event publication (in-process), and any associated metadata updates. Transactional boundaries do not span multiple aggregates or multiple modules. Cross-aggregate consistency is achieved through domain events and the saga pattern, not distributed transactions.

**Related Sections:** 08-cross-cutting-concepts, 09-architecture-decisions (ADR-05, ADR-07)

---

## V

### Value Object

Domain modeling concept representing an immutable, identity-less object defined entirely by its attributes. In CoAgent4U, value objects include `AvailabilityBlock`, `TimeSlot`, `CoordinationId`, `AgentId`, and `EncryptedToken`. Value objects are compared by value, not by reference. They carry no lifecycle and are freely shareable across aggregate boundaries. They enforce domain invariants at construction time (e.g., a `TimeSlot` rejects an end time before a start time).

**Related Sections:** 05-building-block-view

---

*End of 12-glossary.md*
