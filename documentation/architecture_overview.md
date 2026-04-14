# CoAgent4U: Architecture Overview

CoAgent4U is an AI-powered personal agent platform designed for secure, natural-language automation and collaborative meeting scheduling. It is designed around three core pillars: a **Java Modular Monolith Backend**, a **Python Agent Runtime**, and a **Next.js Frontend**.

## 1. High-Level Architecture

The system embraces a **Polyglot Microservices (Modulith + Agent)** approach:

```text
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│                 │       │                 │       │                 │
│ Next.js Web App │ ────► │ Java Spring Boot│ ◄──── │  Python FastMCP │
│ (Frontend UI)   │       │ Modular Monolith│       │  Agent Runtime  │
│                 │       │                 │       │                 │
└─────────────────┘       └────────┬────────┘       └────────┬────────┘
                                   │                         │
                                   ▼                         ▼
                          ┌─────────────────┐       ┌─────────────────┐
                          │   PostgreSQL    │       │  Redis (Memory) │
                          │     (NeonDB)    │       │                 │
                          └─────────────────┘       └─────────────────┘
```

## 2. Core Pillars

### 2.1 Java 21 Modular Monolith (The Orchestrator)
Built on **Spring Boot 3** and strict **Hexagonal Architecture** (Ports and Adapters enforced via ArchUnit), the Java backend consists of 15 fully decoupled Maven modules.
- **Coordination & Sagas:** Handles the complex deterministic state machines required for multi-party meeting scheduling and dual-approval consensus mechanisms without conflicts.
- **Security & Authorization:** Implements JWT-based authentication, AES-256-GCM token encryption for third-party OAuth tokens, rate limiting via Caffeine, and strict Webhook Slack Signature verification.
- **Persistence Layer:** Manages Postgres interactions using Spring Data JPA, partitioned exactly per module to prevent logical data coupling.

### 2.2 Python Agent Runtime (The Intelligence)
The intelligent interface of CoAgent4U, written in **Python 3.11+** utilizing **FastAPI**, **LiteLLM**, and **FastMCP**.
- **Model Context Protocol (MCP):** Connects to the user's Google Calendar and Tasks via 32 predefined tools, providing the LLM Planner with real-time access to the user's schedule without compromising credentials.
- **Model-Agnostic LLM Planner:** Driven by Groq, OpenAI, or local models, performing dynamic Plan → Execute → Summarize loops.
- **Conversational Memory:** Uses Redis to store the last N message exchanges, allowing multi-turn prompt context across agent steps.
- **Privacy Guardian Core (Presidio):** Optionally intercepts outputs and API results to anonymize PII (Personal Identifiable Information) before rendering.

### 2.3 Next.js 16 Frontend (The Interface)
The user control panel built with **Next.js 16**, **React 19**, **Tailwind CSS v4**, and **Radix UI/Shadcn UI**.
- Provides users the ability to perform OAuth connections, manage privacy preferences, view upcoming agenda items (Tasks/Meetings intercept view), and manage outstanding collaborative approvals.

## 3. Communication & Integration
- **Java ↔ Python Bridge:** Communication between the strict Java orchestrator and the LLM Python agent happens via direct internal HTTP calls augmented by shared secrets, allowing asynchronous processing (like Slack webhooks hitting Java → passed to Python → Java notifying user).
- **Agent-to-Agent (A2A) Coordination [ROADMAP]:** The Next Generation roadmap feature. Agents will dynamically communicate over a deterministic protocol (CAP) to negotiate free/busy slots across tenants securely, completely eliminating human-in-the-loop "when are you free" questions constraint by User Privacy Rules.

## 4. Key Data Flows

### Adding a Personal Event
1. User sends Slack Message: "Dentist appointment at 3pm tomorrow"
2. Java Backend parses Webhook → forwards raw text to Python Agent
3. Python Agent LLM formulates a plan, selecting the MCP `create_event` tool.
4. Python Agent executes the MCP tool natively via stored OAuth tokens.
5. Python LLM summarizes "I've added the dentist appointment."
6. Slack is notified of success.

### Collaborative Scheduling (Agent-to-Agent Draft)
1. User A asks Agent A to schedule a sync with User B.
2. Agent A contacts Java Backend to trigger `CoordinationService`.
3. Java Backend creates an asynchronous `IntentDeclaration` payload mapped to Agent B.
4. Agent B's ConsentEngine evaluates privacy rules. If trusted, replies with free slots.
5. Java Backend runs `AvailabilityMatcher` matching the overlap and generates a `MeetingProposal`.
6. Both Users receive an Interactive Approval Prompt.
7. Upon dual-consensus, Java executes an `EventCreationSaga` creating Google Calendar events for both profiles simultaneously.
