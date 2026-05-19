<div align="center">
  <h1>🤖 CoAgent4U</h1>
  <p><strong>AI-powered Personal Agent Platform designed for secure, natural-language automation.</strong></p>
  <p>Manage your schedule, tasks, and productivity seamlessly using your favorite LLM.</p>

  <br/>

  <a href="https://coagent4u.com"><img src="https://img.shields.io/badge/🌐_Live_App-coagent4u.com-00C853?style=for-the-badge" alt="Live App" /></a>

  <br/><br/>

  <!-- Badges -->
  <a href="https://github.com/TanmayZade/CoAgent4U/pulls"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg" alt="PRs Welcome" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21" />
  <img src="https://img.shields.io/badge/Python-3.11+-blue.svg" alt="Python" />
  <img src="https://img.shields.io/badge/Next.js-16-black.svg" alt="Next.js" />
</div>

<br/>

## ✨ Introduction

**CoAgent4U** is an open-source, full-stack AI agent platform that serves as your smart personal assistant.

Unlike simple chatbots, CoAgent4U uses a robust **Model Context Protocol (MCP)** toolset and **Hexagonal Architecture** to securely interact with your real-world services. It can check your schedule, create events, set up task routines, and match availabilities — all without compromising your privacy.

> 🌐 **Try it live at [coagent4u.com](https://coagent4u.com)** — connect your Google Calendar and start managing your schedule with natural language.

## 🏛️ Architecture

CoAgent4U is a **polyglot modulith** — three purpose-built runtimes working in concert:

```text
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│                 │       │                 │       │                 │
│ Next.js Web App │ ────► │ Java Spring Boot│ ◄──── │  Python FastMCP │
│ (Frontend UI)   │       │ Modular Monolith│       │  Agent Runtime  │
│                 │       │  (15 modules)   │       │  (32 MCP Tools) │
└─────────────────┘       └────────┬────────┘       └────────┬────────┘
                                   │                         │
                                   ▼                         ▼
                          ┌─────────────────┐       ┌─────────────────┐
                          │   PostgreSQL    │       │  Redis (Memory) │
                          └─────────────────┘       └─────────────────┘
```

| Pillar | Role | Stack |
|--------|------|-------|
| **Java Modulith** | System of record — auth, persistence, state machines, Slack integration | Java 21, Spring Boot 3, Hexagonal/DDD, ArchUnit |
| **Python Agent** | Intelligence layer — LLM planning, MCP tool execution, Google API | FastAPI, LiteLLM, FastMCP, Presidio |
| **Next.js Frontend** | User dashboard — OAuth, agenda, approvals, coordination | Next.js 16, React 19, Tailwind CSS, Shadcn UI |

## 🚀 Key Features

- **🧠 Autonomous LLM Planner:** A model-agnostic planner (supporting Groq, OpenAI, Claude, local models) orchestrates complex tasks using a dynamic **plan → execute → summarize** loop.
- **🛠️ Rich Tool Ecosystem (MCP):** Comes pre-built with **32 tools** spanning calendar management (17), task tracking (8), and productivity optimization (7) via Google OAuth.
- **🔒 Privacy First & Secure:** Multi-layer security combining JWT authentication, AES-256-GCM encryption, Slack signature verification, and Microsoft Presidio for PII anonymization.
- **🧩 Modular Monolith:** A clean, extensible **15-module** Java Spring Boot backend built on Domain-Driven Design (DDD) and Hexagonal Architecture principles.
- **💬 Conversational Memory:** Redis-backed multi-turn context allows continued, natural conversations with your agent across sessions.
- **🤝 A2A Coordination:** Agent-to-Agent protocol support for secure, cross-user meeting scheduling without manual back-and-forth.

## 🛠️ Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend (Java)** | Java 21, Spring Boot 3, Hexagonal Architecture, ArchUnit, Flyway |
| **Backend (Python)** | FastAPI, LiteLLM, FastMCP, Presidio (Privacy) |
| **Frontend** | Next.js 16, React 19, TailwindCSS v4, Shadcn UI |
| **Infrastructure** | PostgreSQL, Redis, Docker, Testcontainers |
| **Security** | JWT (HS256), AES-256-GCM, HMAC-SHA256 (Slack), Caffeine Rate Limiter |

---

## 🏁 Getting Started

### Prerequisites

- **Java:** 21 or higher
- **Maven:** 3.9.0 or higher
- **Python:** 3.11 or higher
- **Node.js:** 20+ (for frontend)
- **Docker & Docker Compose** (for running local PostgreSQL & Redis)

### 1. Clone & Configure

```bash
git clone https://github.com/TanmayZade/CoAgent4U.git
cd CoAgent4U

cp .env.example .env
# On Windows: copy .env.example .env
```

Fill in the `.env` file with your configuration:

- **Database:** Local PostgreSQL or remote (NeonDB)
- **Google OAuth:** Client ID, Client Secret, Redirect URI
- **Security:** JWT secret (min 32 chars), AES-256 encryption key (base64)
- **LLM:** Your Groq / OpenAI / Anthropic API key

### 2. Start Infrastructure

```bash
docker-compose up -d
```

This brings up PostgreSQL and Redis locally.

### 3. Build & Run the Java Backend

```bash
mvn clean install
mvn spring-boot:run -pl coagent-app
```

The Java backend starts on `http://localhost:8080`.

### 4. Start the Python Agent

```bash
cd agent-py
pip install -r requirements.txt
python -m app.main
```

The Python agent starts on `http://localhost:8000`.

### 5. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The dashboard is available at `http://localhost:3000`.

---

## 📁 Project Structure

```
CoAgent4U/
├── shared-kernel/          # Value objects (AgentId, Email, TimeRange, etc.)
├── common-domain/          # Domain events infrastructure
├── core/
│   ├── agent-module/       # Agent entity, intent parsing, conflict detection
│   ├── coordination-module/ # State machine, availability matching, sagas
│   ├── approval-module/    # Dual-consensus approval engine
│   └── user-module/        # User management, workspace onboarding
├── integration/
│   ├── calendar-module/    # Google Calendar adapter
│   ├── messaging-module/   # Slack inbound/outbound adapters
│   ├── llm-module/         # Groq LLM adapter
│   └── a2a-module/         # Agent-to-Agent protocol adapter
├── infrastructure/
│   ├── persistence/        # JPA entities, repositories, mappers
│   ├── security/           # JWT, AES, Slack signature, rate limiting
│   ├── config/             # Spring profiles & app configuration
│   └── monitoring/         # Health checks, metrics
├── coagent-app/            # Composition root (Spring Boot application)
├── agent-py/               # Python agent runtime (FastAPI + MCP + LLM)
├── frontend/               # Next.js 16 dashboard
└── documentation/          # Architecture docs, PRD, guides
```

---

## 🔄 How It Works

### Adding a Personal Event

```
User → "Dentist appointment at 3pm tomorrow"
  ↓
Slack Webhook → Java Backend → Python Agent
  ↓
LLM plans → selects `create_event` MCP tool
  ↓
Google Calendar API creates event
  ↓
LLM summarizes → "✅ I've added your dentist appointment for tomorrow at 3 PM."
  ↓
Slack notifies user
```

### Collaborative Scheduling (A2A)

```
User A → "Schedule a sync with User B"
  ↓
Agent A → Java Coordination Service → Agent B
  ↓
Agent B evaluates privacy rules → shares free slots
  ↓
AvailabilityMatcher finds overlap → MeetingProposal
  ↓
Both users receive approval prompts
  ↓
Dual-consensus → EventCreationSaga → Events on both calendars
```

---

## 🤝 Contributing

We ❤️ contributions! Whether you're a beginner looking for your first PR or an experienced engineer wanting to add a new MCP tool, you are welcome.

1. **Fork** the project & branch from `main`.
2. **Build and test** your changes locally.
3. Submit a **Pull Request** explaining your changes.
4. Check the `issues` tab for `good-first-issue` tags!

If you encounter a bug or have a feature idea, feel free to [open an issue](https://github.com/TanmayZade/CoAgent4U/issues)!

## 🗺️ Roadmap

We are constantly evolving CoAgent4U. Here's what's coming:

- [ ] **Agent-to-Agent (A2A) Coordination:** Full implementation of the [A2A Protocol](https://github.com/a2aproject/A2A) for secure cross-agent negotiation.
- [ ] **Local Model Support:** One-click setups for Ollama and Gemma to keep operations fully local.
- [ ] **Expanded Integrations:** Notion, GitHub, and Jira tool sets via MCP.
- [ ] **Desktop Client:** Bringing the agent to your menubar/taskbar.
- [ ] **Mobile App:** React Native companion for on-the-go scheduling.

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
