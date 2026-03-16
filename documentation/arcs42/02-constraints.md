# documentation/arcs42/02-constraints.md

---

## Table of Contents

- [1. Technical Constraints](#1-technical-constraints)
  - [1.1 Backend Constraints](#11-backend-constraints)
  - [1.2 Frontend Constraints](#12-frontend-constraints)
  - [1.3 Infrastructure Constraints](#13-infrastructure-constraints)
  - [1.4 Third-Party Service Constraints](#14-third-party-service-constraints)
- [2. Organizational Constraints](#2-organizational-constraints)
- [3. Political and Regulatory Constraints](#3-political-and-regulatory-constraints)
- [4. Conventions](#4-conventions)

---

## 1. Technical Constraints

These constraints are derived from the selected technology stack and the architectural decisions defined in Section 09 – Architecture Decisions.


### 1.1 Backend Constraints

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| TC-01 | Java 21 (LTS) | The backend runtime is Java 21 with JVM tuning for performance. All domain, application, and adapter code is written in Java. |
| TC-02 | Spring Boot 3.x | The framework layer uses Spring Boot 3.x, specifically Spring Web for REST APIs, Spring Data JPA for ORM, Spring Security for authentication and authorization, and Spring Actuator for monitoring. Spring is confined to the adapter and configuration layers only — the domain layer must have zero Spring imports. |
| TC-03 | Maven 3.9+ | Build and dependency management uses Maven 3.9+. All modules are managed as Maven modules within a single multi-module project. |
| TC-04 | PostgreSQL 15+ | Primary persistence is PostgreSQL 15+. All coordination state, agent activitys, user data, and approval records are stored in PostgreSQL using ACID transactions. |
| TC-05 | Flyway | All database schema changes are managed through Flyway migrations. No manual DDL is permitted against any environment. |
| TC-06 | Caffeine | In-memory caching uses Caffeine strictly for read optimization and may cache user profiles, service connections, parsed intent results, or availability lookups. It must not store authoritative workflow state, including coordination state machine state, approval decisions, or saga execution progress. All authoritative workflow state must be persisted via PersistencePort before any external side effects occur, and cache loss must not affect correctness. |
| TC-07 | Spring WebClient | All outbound HTTPS calls (Slack API, Google Calendar API, Groq API) use Spring WebClient, which is reactive and non-blocking. |
| TC-08 | Hexagonal Domain Purity | The domain module must have zero dependencies on Spring, JPA annotations, or any framework. Domain classes are plain Java objects. All persistence and external communication happens through port interfaces implemented by adapter classes in the infrastructure layer. |
| TC-09 | Deterministic Coordination Protocol | The coordination-module implements a pure deterministic state machine and negotiation protocol and is not an ingress boundary; it may only be invoked by the agent-module through CoordinationProtocolPort. It must not subscribe directly to approval events, access CalendarPort, UserQueryPort, or any infrastructure port, nor access other modules’ persistence. All coordination state transitions must be persisted transactionally before any external side effects occur, and LLM usage is strictly confined to the agent-module behind LLMPort.|


### 1.2 Frontend Constraints

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| TC-10 | React 18 with TypeScript | The frontend is built with React 18 and TypeScript. All components must be typed. |
| TC-11 | ShadcnUI | UI components use ShadcnUI, which is Tailwind-based. No additional CSS frameworks are introduced. |
| TC-12 | React Context API and React Query | Client state is managed via React Context API. Server state (API data fetching, caching, synchronization) is managed via React Query. No Redux or other state libraries are permitted. |
| TC-13 | Vite | The frontend build tool is Vite. |
| TC-14 | React Router v6 | Client-side routing uses React Router v6. |
| TC-15 | Strict TypeScript Configuration | TypeScript must run in strict mode (strict: true). No implicit any allowed. |

### 1.3 Infrastructure Constraints

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| TC-16 | Docker | The application is containerized using Docker. Local development uses docker-compose to orchestrate the application container and the PostgreSQL container with persistent volumes. |
| TC-17 | Single Deployable Artifact | The system is deployed as a modular monolith — one Docker image containing the Spring Boot application. There are no microservices in the MVP. |
| TC-18 | Reverse Proxy (Future) | Nginx or Traefik is planned for future use as a reverse proxy. This is not part of the MVP deployment but the architecture must not preclude it. |
| TC-19 | Orchestration (Future) | Kubernetes is planned for post-MVP orchestration. The MVP must run correctly without Kubernetes, but the containerization strategy must be Kubernetes-compatible. |

### 1.4 Third-Party Service Constraints

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| TC-20 | Slack API | User interaction is exclusively through Slack using the Events API, Messaging API, and Interactive Components. Slack OAuth is used for workspace installation. The system must acknowledge Slack events within 3 seconds or Slack will retry, so long-running operations must be handled asynchronously. |
| TC-21 | Google Calendar API | Calendar read and write operations use the Google Calendar API. Authentication uses Google OAuth 2.0. The system must handle token refresh, API rate limits, and request only the minimum required OAuth scopes. Calendar access is mediated exclusively by the agent-module via CalendarPort. The coordination-module must not depend on CalendarPort. |
| TC-22 | Groq API | LLM-based intent classification uses the Groq API, accessed through the GroqLLMAdapter implementing the LLMPort interface. The system must handle Groq API latency, rate limits, and downtime gracefully via timeouts and the rule-based parser fallback described in 01-introduction-and-goals.md Section 3. |
| TC-23 | Secrets Management | The MVP uses environment variables for secrets (API keys, OAuth client secrets, database credentials). The architecture must support migration to HashiCorp Vault or AWS Secrets Manager in the future without changes to domain or application code. |
| TC-24 | Agent Sovereignty Enforcement | All user-originated and user-scoped operations must pass through the agent-module. The coordination-module must not receive external events, must not subscribe directly to approval events, and must not depend on infrastructure ports such as CalendarPort or UserPersistencePort. |


---

## 2. Organizational Constraints

These constraints arise from the project team structure, budget, and timeline realities.

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| OC-01 | Founding Team | The entire system — architecture, backend, frontend, infrastructure, security, and operations — is built and maintained by a Founding Team. This means operational complexity must be minimized, CI/CD must be fully automated, and manual deployment steps are not acceptable. |
| OC-02 | MVP Scope Lock | The MVP delivers exactly two use cases as defined in [01-introduction.md S3 – MVP Use Cases][01-introduction-and-goals.md S3 – MVP Scope](./01-introduction-and-goals.md#3-mvp-scope). No additional use cases, integrations (e.g., Outlook, Microsoft Teams), or agent capabilities are in scope. Feature requests are deferred to a documented backlog. |
| OC-03 | Budget Minimization | Infrastructure costs must remain minimal. This drives the choice of a modular monolith over microservices, the use of Caffeine over Redis for MVP caching, environment variables over a secrets management service, and the two-tier intent parsing strategy that minimizes LLM API calls. |
| OC-04 | Open Source Preference | Battle-tested open-source libraries are preferred over paid SaaS solutions for all concerns except the three core external APIs (Slack, Google Calendar, Groq). |

---

## 3. Political and Regulatory Constraints

These constraints are imposed by legal requirements and third-party service policies that the system cannot negotiate or override.

| Constraint ID | Constraint | Description |
|:---|:---|:---|
| PC-01 | GDPR Compliance | The system processes personal data including names, email addresses, and calendar event details. GDPR requires encryption of data at rest and in transit, a user's right to export their data, a user's right to request deletion of their data (Right to Erasure), data minimization (collect only what is necessary), and clear documentation of what data is stored and why. These requirements are non-negotiable and must be implemented in the MVP, not deferred. |
| PC-02 | Google API Services User Data Policy | Google imposes strict requirements on how user data obtained through its APIs is accessed, used, stored, and shared. The application must request minimum necessary OAuth scopes, must not share Google user data with third parties without consent, and must undergo Google verification if it serves more than 100 users. |
| PC-03 | Slack API Terms of Service | Slack requires that apps installed in workspaces handle user data responsibly, provide clear descriptions of what the app does, and do not abuse the Events API or Messaging API through excessive calls. Rate limits must be respected. |
| PC-04 | AgentActivityability as a Trust Requirement | Every action taken by an agent on behalf of a user must be traceable with full provenance — who initiated it, what was parsed, what state transitions occurred, what approvals were given, and what calendar mutations resulted. This is both a legal requirement for GDPR accountability and a product trust requirement. The agent activity is append-only and immutable. |

---

## 4. Conventions

The following conventions are enforced across the codebase to maintain consistency and uphold the Hexagonal Architecture constraints in a founding team context where code review is self-directed.

**Dependency Rule:** Source code dependencies must only point inward. Adapter classes depend on application-layer port interfaces. Application-layer use cases depend on domain entities and domain services. The domain layer depends on nothing — no Spring annotations, no JPA annotations, no external library imports. This rule is enforced through Maven module boundaries where the domain module has no dependency on the adapter or infrastructure modules.

**Package Structure:** Each bounded module follows a consistent package layout: `domain` (entities, value objects, domain services, domain events), `application` (use cases, port interfaces, DTOs), and `adapter` (inbound adapters such as REST controllers and Slack handlers, outbound adapters such as repository implementations and API clients).

**Ubiquitous Language:** Class names, method names, and variable names must reflect the business domain language. Examples include `CoordinationSaga`, `TimeSlot`, `ApprovalDecision`, `IntentClassification`, and `CalendarConflict`. Technical jargon in domain code (e.g., `handleRequest`, `processData`) is avoided in favor of business-meaningful names.

**Architecture Decision Records:** Every significant architectural decision must be documented as an ADR in `docs/arc42/09-architecture-decisions.md` before the corresponding implementation begins. An ADR includes the context, the decision made, the alternatives considered, and the consequences.

**Testing Conventions:** Domain layer tests are pure unit tests with no mocks and no Spring context — they test plain Java logic. Application layer tests use mocked port interfaces to verify use case orchestration. Adapter layer tests are integration tests using Testcontainers for PostgreSQL and WireMock for external API simulation. End-to-end tests cover only the critical happy paths for the two MVP use cases.

**Database Migration Naming:** Flyway migration files follow the naming convention `V{version}__{description}.sql`, for example `V001__create_users_table.sql`. Every schema change is a forward migration. Destructive changes require explicit documentation in the corresponding ADR.

**Git Conventions:** Commits follow Conventional Commits format (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`). The `main` branch is always deployable. Feature work is done on short-lived branches merged via squash merge.

---

*End of 02-constraints.md*
