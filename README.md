<div align="center">
  <h1>🤖 CoAgent4U</h1>
  <p><strong>AI-powered Personal Agent Platform designed for secure, natural-language automation.</strong></p>
  <p>Manage your schedule, tasks, and productivity perfectly using your favorite LLM.</p>

  <!-- Badges -->
  <a href="https://github.com/yourusername/CoAgent4U/pulls"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg" alt="PRs Welcome" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21" />
  <img src="https://img.shields.io/badge/Python-3.11+-blue.svg" alt="Python" />
  <img src="https://img.shields.io/badge/Next.js-16-black.svg" alt="Next.js" />
</div>

<br/>

## ✨ Introduction

Welcome to **CoAgent4U**! This project is an open-source, full-stack AI agent platform that serves as your smart personal assistant. 

Unlike simple chatbots, CoAgent4U uses a robust **Model Context Protocol (MCP)** toolset and **Hexagonal Architecture** to securely interact with your real-world services. It can check your schedule, create events, set up task routines, matching availabilities without compromising your privacy.

## 🚀 Key Features

- **🧠 Autonomous LLM Planner:** A model-agnostic planner (supporting Groq, OpenAI, Claude, local models) orchestrates complex tasks using a dynamic plan → execute → summarize loop.
- **🛠️ Rich Tool Ecosystem (MCP):** Comes pre-built with 32 tools spanning calendar management, task tracking, and productivity optimization via Google OAuth.
- **🔒 Privacy First & Secure:** Includes end-to-end multi-layer security combining JWT token authentication, AES-256-GCM encryption, Slack signature verification, and Microsoft Presidio for strict PII anonymization.
- **🧩 Modular Monolith:** A highly clean, extensible 15-module Java Spring Boot backend built on Domain-Driven Design (DDD) principles.
- **💬 Conversational Memory:** Redis-backed multi-turn context allows you to have continued, natural conversations with your agent.

## 🛠️ Tech Stack

- **Backend (Python):** FastAPI, LiteLLM, FastMCP, Presidio (Privacy).
- **Backend (Java):** Java 21, Spring Boot 3, Hexagonal Architecture, ArchUnit.
- **Frontend:** Next.js 16, React 19, TailwindCSS, ShadcnUI.
- **Infrastructure:** PostgreSQL, Redis, Docker, Testcontainers.

---

## 🏁 Getting Started

We've made setting up CoAgent4U as simple as possible for newcomers. 

### Prerequisites

- **Java:** 21 or higher
- **Maven:** 3.9.0 or higher
- **Python:** 3.11 or higher
- **Node.js:** 20+ (for frontend)
- **Docker & Docker Compose** (Optional, for running local PostgreSQL & Redis)

### 1. Environment Configuration

Clone the repository and prepare your environment file:

```bash
cp .env.example .env
# On Windows: copy .env.example .env
```
Fill in the `.env` file with your specific configuration values:
- Database (Remote NeonDB or Local)
- Google OAuth credentials
- Security Secrets (JWT, AES)
- LLM API Keys (Groq, etc.)

### 2. Database & Cache Setup

Run the required infrastructure locally via Docker:
```bash
docker-compose up -d
```
*(Updates your `DATABASE_URL` to point to `localhost:5432` if you choose this local route).*

### 3. Build & Run the Backend

To build the Java core:
```bash
mvn clean install
```
Start the application:
```bash
mvn spring-boot:run -pl coagent-app
```

---

## 🤝 How to Contribute

We ❤️ contributions! Whether you're a beginner looking for your first PR or an experienced engineer wanting to add a new MCP tool, you are highly encouraged to contribute.

1. **Fork** the project & branch from `main`.
2. **Build and test** your changes locally.
3. Submit a **Pull Request** explaining your changes.
4. Don't forget to check our `issues` tab for "good-first-issue" tags!

If you encounter a bug or have a feature idea, feel free to **open an issue**!

## 🗺️ Roadmap & What's Next

We are constantly evolving CoAgent4U. Here's a sneak peek into the major updates coming in the next versions:

- [ ] **Agent-to-Agent (A2A) Coordination [NEXT VERSION]:** Empower personal agents to communicate directly with each other to securely negotiate meeting times, hand off tasks, and share context seamlessly across multiple users.
- [ ] **Local Model Support Native Integration:** One-click setups for Ollama and Gemma to keep operations fully local.
- [ ] **Expanded Integrations:** Notion, GitHub, and Jira tool sets via MCP.
- [ ] **Extensible Desktop Client:** Bringing the agent right to your menubar/taskbar.

## 📄 License

This project is licensed under the MIT License - see the `LICENSE` file for details.
