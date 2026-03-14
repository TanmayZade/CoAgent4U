# CoAgent4U — Dashboard Architecture Plan

> **Step 1: Architecture Planning** — Product thinking, not UI visuals.

---

## Research Basis

This architecture is derived from:

- **PRD** — Sections 4.1 (Scope), 5.2 (User Journeys), 6.1–6.7 (Functional Requirements), UC-001 through UC-004
- **API Surface** — [API_DOCUMENTATION.md](file:///e:/CoAgent4U/frontend/API_DOCUMENTATION.md) (all authenticated endpoints)
- **Domain Model** — 4 modules: `user-module`, `agent-module`, `coordination-module`, `approval-module`
- **Existing Frontend** — Next.js app with routes for `/dashboard`, `/dashboard/audit`, `/dashboard/calendar`, `/dashboard/coordinations`, `/dashboard/data`

---

## Global Dashboard Structure

The dashboard has **one persistent shell** and **5 core sections**, each corresponding to a distinct user need from the PRD.

```
┌──────────────────────────────────────────────────────┐
│  SIDEBAR NAV              │  MAIN CONTENT AREA       │
│                           │                          │
│  ● Home (Overview)        │  [Active Section]        │
│  ● Agent Activity         │                          │
│  ● Coordinations          │                          │
│  ● Integrations           │                          │
│  ● Settings               │                          │
│                           │                          │
│  ─────────────────        │                          │
│  User Profile Card        │                          │
│  Theme Toggle             │                          │
│  Logout                   │                          │
└──────────────────────────────────────────────────────┘
```

---

## Section 1 → Home (Overview)

### Purpose

The default landing state after login. Gives the user an **instant snapshot** of their agent's world — what just happened, what needs attention, and overall system health. This is **not** a feature page; it's a **situational awareness** surface.

### What It Shows

| Element | Data Source | PRD Mapping |
|---------|-----------|-------------|
| Active coordination banner (if any) | Coordination state (API: future endpoint or local state) | REQ-COORD-011 |
| Pending approvals count | Derived from coordination state | REQ-APPR-001/002 |
| Agent status indicator | `GET /me` → `googleCalendarConnected` | REQ-AUTH-004 |
| Google Calendar connection status | `GET /integrations/google/status` | REQ-AUTH-003 |
| Slack App installation status | `GET /auth/me` → `isSlackAppInstalled` | REQ-AUTH-001 |
| Quick-start CTA (if setup incomplete) | Derived from above checks | UC-003 |

### User Actions

- View at-a-glance system health
- Tap into any active coordination to see details
- Navigate to Integrations if setup is incomplete
- See if agent is ready to receive Slack commands

### Why It Exists

Without a home view, the user has no orientation point. Premium SaaS products (Linear, Notion) always give users a single screen that answers: *"Where am I? What needs my attention?"* This section answers that.

---

## Section 2 → Agent Activity

### Purpose

A chronological log of **everything the user's personal agent has done** on their behalf. This is the transparency and auditability surface required by the PRD (REQ-DATA-003, REQ-DATA-004).

### What It Shows

| Element | Data Source | PRD Mapping |
|---------|-----------|-------------|
| Activity feed (timeline) | Audit log / activity history (API: future endpoint for agent actions log) | REQ-DATA-003 |
| Action type badges (Personal Event, Coordination, Conflict, Error) | Categorized from activity data | REQ-DATA-004 |
| Each entry: timestamp, action description, outcome | Structured audit log entries | REQ-DATA-002 |
| Filter/search by type and date range | Client-side filtering on fetched data | REQ-USE-002 |

### User Actions

- Browse agent action history
- Filter by action type (personal events, coordinations, errors)
- Review what data the agent accessed and when (GDPR transparency)

### Why It Exists

**PRD REQ-DATA-003** and **REQ-DATA-004** are P0 requirements: users must have an audit trail of agent actions and visibility into data access. This is not optional — it's a GDPR compliance surface and a trust-building feature. Users who see what their agent did will trust the system more.

> [!IMPORTANT]
> This section depends on a **backend audit/activity log endpoint** that does not yet exist in the current API surface. The backend currently logs events but doesn't expose them via REST. An API like `GET /me/activity` or `GET /agent/activity` will be needed.

---

## Section 3 → Coordinations

### Purpose

A dedicated view for **all A2A coordination sessions** — past, active, and completed. This is the power-user view for anyone who wants to review multi-agent scheduling flows.

### What It Shows

| Element | Data Source | PRD Mapping |
|---------|-----------|-------------|
| Coordination list (table/cards) | Coordination records (API: future endpoint) | REQ-COORD-011 |
| Each entry: participants, status, proposed time, outcome | Coordination aggregate data | REQ-COORD-002 |
| Status badges: Initiated, Awaiting Approval, Completed, Rejected, Failed | CoordinationState enum | REQ-COORD-011 |
| Coordination detail view (expandable or drill-down) | Full coordination + state log | REQ-DATA-002 |
| State transition timeline (within detail view) | `coordination_state_log` | REQ-COORD-002 |

### User Actions

- View all past and current coordinations
- Filter by status (Active, Completed, Rejected, Failed)
- Drill into a specific coordination to see the full state machine timeline
- Understand why a coordination failed or was rejected

### Why It Exists

The 10-step deterministic coordination protocol is the **core innovation** of CoAgent4U (PRD Section 1). Users need a place to understand what happened during multi-agent negotiations. This also addresses **REQ-DATA-002** (coordination log storage must be queryable by user).

> [!IMPORTANT]
> This section requires a **backend endpoint** to list and retrieve coordination records for the authenticated user, e.g., `GET /me/coordinations` and `GET /me/coordinations/{id}`. This does not exist in the current API surface.

---

## Section 4 → Integrations

### Purpose

A settings-adjacent surface for managing **connected services** — currently Google Calendar, and potentially more in the future. This is the "plumbing" view: connect, disconnect, and verify service health.

### What It Shows

| Element | Data Source | PRD Mapping |
|---------|-----------|-------------|
| Google Calendar connection card | `GET /integrations/google/status` | REQ-CAL-001 |
| Connection status badge (Connected / Not Connected) | Status response | REQ-AUTH-003 |
| Connect / Disconnect actions | `GET /integrations/google/authorize` / `DELETE /integrations/google/disconnect` | REQ-AUTH-003 |
| Slack App installation status | `GET /auth/me` → `isSlackAppInstalled` | REQ-AUTH-001 |
| Install Slack App CTA (if not installed) | `GET /auth/slack/install/start` | REQ-SLACK-001 |
| Scope/permissions transparency | Static display of granted scopes | REQ-DATA-004 |

### User Actions

- Connect Google Calendar (OAuth flow)
- Disconnect Google Calendar
- Install Slack App to workspace (if admin)
- View what permissions each service has
- Understand data scope (read/write calendar events)

### Why It Exists

**REQ-AUTH-003** requires a "Connect Google Calendar" interface. **REQ-DATA-004** requires connected services to be displayed with scope explanations. **UC-004** (View Data Access & Permissions) explicitly specifies this as a user flow. All current APIs for this section already exist.

---

## Section 5 → Settings

### Purpose

User profile, account controls, data management, and privacy actions. This is the **account governance** center.

### What It Shows

| Element | Data Source | PRD Mapping |
|---------|-----------|-------------|
| User profile card (username, email, avatar, workspace) | `GET /auth/me` | REQ-AUTH-002 |
| Account creation date | `GET /me` → `createdAt` | — |
| Theme toggle (Dark/Light) | Client-side preference | Design constraint |
| Delete Account (with confirmation) | Future endpoint: `DELETE /me` | REQ-DATA-005 |
| Data export (future) | Future endpoint | GDPR |
| Logout | `POST /auth/logout` | REQ-SEC-005 |

### User Actions

- View profile information
- Toggle dark/light mode
- Delete account and all data (GDPR right to erasure)
- Logout

### Why It Exists

**REQ-DATA-005** (P0) mandates a "Delete Account" option. **UC-004** includes user ability to "delete account and all data." The theme toggle supports the dark/light mode design constraint. Profile display provides identity confirmation.

> [!NOTE]
> Account deletion (`DELETE /me`) does not exist in the current API. It needs to be built backend-side to cascade delete user, agent, connections, approvals, and coordination references (GDPR: 30-day completion window per PRD REQ-DATA-005).

---

## Navigation Structure

### Sidebar (Persistent)

```
◆ Home               → /dashboard
◆ Agent Activity      → /dashboard/activity
◆ Coordinations       → /dashboard/coordinations
◆ Integrations        → /dashboard/integrations
◆ Settings            → /dashboard/settings
────────────
[User Avatar + Name]
[Theme Toggle]
[Logout]
```

### Design Principles

- **Collapsible sidebar** — Expands to show labels, collapses to icon-only for more content space
- **No nested navigation** — Every section is one click from the sidebar. Drill-down happens within the main content area (e.g., coordination detail)
- **Active state indicator** — Subtle highlight on the current section
- **Mobile responsive** — Sidebar becomes a bottom tab bar or hamburger menu on smaller viewports

---

## Key User Flows

### Flow 1: First-Time Setup

```
Login → Home (Overview)
  └─ Sees setup checklist:
      ✗ Google Calendar not connected
      ✗ Slack App not installed
  └─ Clicks "Connect Google Calendar" → OAuth flow → Returns to Home
  └─ Clicks "Install Slack App" → Slack flow → Returns to Home
  └─ Home now shows: ✓ All systems ready → "Go to Slack to start using CoAgent4U"
```

**PRD mapping:** UC-003 (Onboarding Journey steps 6–13)

---

### Flow 2: Reviewing Agent Activity

```
Home → Agent Activity
  └─ Sees chronological feed of agent actions
  └─ Filters by "Coordination" type
  └─ Sees a failed coordination
  └─ Clicks to drill into Coordinations section for detail
```

**PRD mapping:** REQ-DATA-003, REQ-DATA-004

---

### Flow 3: Monitoring an Active Coordination

```
Home → Sees "Active Coordination" banner
  └─ Clicks banner → Coordinations section
  └─ Sees coordination in "AWAITING_APPROVAL_B" state
  └─ Views state transition timeline
  └─ Waits for User B to approve in Slack
  └─ Returns later → Status updated to "COMPLETED"
```

**PRD mapping:** REQ-COORD-011, UC-002

---

### Flow 4: Managing Integrations

```
Home → Integrations
  └─ Sees Google Calendar: Connected ✓
  └─ Clicks "Disconnect" → Confirmation modal → Disconnected
  └─ Clicks "Reconnect" → OAuth flow → Connected again
```

**PRD mapping:** REQ-AUTH-003, REQ-CAL-001

---

### Flow 5: Account & Data Governance (GDPR)

```
Home → Settings
  └─ Reviews profile information
  └─ Clicks "Delete Account"
  └─ Confirmation modal with re-authentication
  └─ Account deletion initiated (30-day GDPR window)
```

**PRD mapping:** REQ-DATA-005, GDPR Section 15.3

---

## Backend API Readiness

| Section | Available APIs | Missing APIs (Need Backend Work) |
|---------|---------------|----------------------------------|
| **Home** | `GET /auth/me`, `GET /me`, `GET /integrations/google/status` | — |
| **Agent Activity** | — | `GET /me/activity` (audit log endpoint) |
| **Coordinations** | — | `GET /me/coordinations`, `GET /me/coordinations/{id}` |
| **Integrations** | `GET /integrations/google/status`, `GET /integrations/google/authorize`, `DELETE /integrations/google/disconnect`, `GET /auth/slack/install/start` | — |
| **Settings** | `GET /auth/me`, `GET /me`, `POST /auth/logout` | `DELETE /me` (account deletion) |

---

## What Is Deliberately Excluded

Based on the PRD "Out of Scope (MVP)" and the minimalism constraint:

| Excluded Feature | Reason |
|------------------|--------|
| Calendar event list/viewer | Calendar data is queried on-demand by the agent, not stored. No API exposes calendar events to the frontend. The calendar is managed entirely in Google Calendar. |
| Chat/messaging interface | All agent interaction happens in Slack (PRD Section 6.4). The dashboard is a **control plane**, not a communication tool. |
| Analytics/metrics charts | No backend metrics API exists for MVP. Out of scope per PRD Section 4.2. |
| User roles/team management | Single role per PRD 4.2. No admin hierarchy in MVP. |
| Notification center | All notifications go through Slack (PRD REQ-SLACK-004). Dashboard is not a notification surface. |
| Agent configuration/preferences | No agent preferences API exists. Deferred to post-MVP. |

---

## Summary

| # | Section | Core Purpose | PRD Anchor |
|---|---------|-------------|------------|
| 1 | **Home** | Situational awareness & setup status | UC-003, REQ-AUTH-001/003/004 |
| 2 | **Agent Activity** | Transparency & audit trail | REQ-DATA-003, REQ-DATA-004 |
| 3 | **Coordinations** | Coordination history & detail | REQ-COORD-011, REQ-DATA-002 |
| 4 | **Integrations** | Service connections & permissions | REQ-AUTH-003, REQ-CAL-001, UC-004 |
| 5 | **Settings** | Account governance & GDPR | REQ-DATA-005, GDPR 15.3 |
