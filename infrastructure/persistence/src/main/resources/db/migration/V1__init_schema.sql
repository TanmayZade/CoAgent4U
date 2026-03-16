-- ============================================================================
-- V1: CoAgent4U – Full Schema Baseline
-- Generated: 2026-03-15
-- ============================================================================
-- This migration creates the entire database schema from scratch.
-- Tables are ordered by dependency:  independent tables first, then dependents.
-- No ALTER statements – every column is in its final form.
-- ============================================================================

-- ==========================================================================
-- 1. USERS
-- Owner: user-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id     UUID            PRIMARY KEY,
    username    VARCHAR(64),
    email       VARCHAR(255)    NOT NULL UNIQUE,
    preferences JSONB,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
    ON users (username) WHERE username IS NOT NULL;

-- ==========================================================================
-- 2. SLACK IDENTITIES  (child of users)
-- Owner: user-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS slack_identities (
    slack_user_id       VARCHAR(64)     PRIMARY KEY,
    workspace_id        VARCHAR(64)     NOT NULL,
    workspace_name      VARCHAR(255),
    workspace_domain    VARCHAR(255),
    email               VARCHAR(255),
    display_name        VARCHAR(255),
    avatar_url          TEXT,
    linked_user_id      UUID            NOT NULL REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_slack_identities_user_id
    ON slack_identities (linked_user_id);

-- ==========================================================================
-- 3. SERVICE CONNECTIONS  (child of users)
-- Owner: user-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS service_connections (
    connection_id           UUID            PRIMARY KEY,
    user_id                 UUID            NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    service_type            VARCHAR(32)     NOT NULL,
    encrypted_token         TEXT            NOT NULL,
    encrypted_refresh_token TEXT,
    token_expires_at        TIMESTAMPTZ,
    status                  VARCHAR(16)     NOT NULL DEFAULT 'CONNECTED',
    connected_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    google_account_id       VARCHAR(128),
    disconnected_at         TIMESTAMPTZ,
    CONSTRAINT chk_service_type      CHECK (service_type IN ('GOOGLE_CALENDAR')),
    CONSTRAINT chk_connection_status CHECK (status IN ('CONNECTED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_service_connections_user_id
    ON service_connections (user_id);
CREATE INDEX IF NOT EXISTS idx_service_connections_status
    ON service_connections (user_id, service_type, status);

-- ==========================================================================
-- 4. AGENTS
-- Owner: agent-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS agents (
    agent_id    UUID            PRIMARY KEY,
    user_id     UUID            NOT NULL,
    status      VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_agents_user_id
    ON agents (user_id);

-- ==========================================================================
-- 5. COORDINATIONS
-- Owner: coordination-module
-- No CHECK on state – domain CoordinationStateMachine is the single source of truth.
-- ==========================================================================
CREATE TABLE IF NOT EXISTS coordinations (
    coordination_id     UUID            PRIMARY KEY,
    requester_agent_id  UUID            NOT NULL,
    invitee_agent_id    UUID            NOT NULL,
    state               VARCHAR(32)     NOT NULL,
    proposal            JSONB,
    reason              VARCHAR(255),
    available_slots_json JSONB,
    selected_slot_json  JSONB,
    metadata_json       JSONB,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_coordinations_requester
    ON coordinations (requester_agent_id);
CREATE INDEX IF NOT EXISTS idx_coordinations_invitee
    ON coordinations (invitee_agent_id);
CREATE INDEX IF NOT EXISTS idx_coordinations_state
    ON coordinations (state);

-- ==========================================================================
-- 6. COORDINATION STATE LOG  (child of coordinations)
-- Owner: coordination-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS coordination_state_log (
    log_id              UUID            PRIMARY KEY,
    coordination_id     UUID            NOT NULL REFERENCES coordinations (coordination_id),
    from_state          VARCHAR(32)     NOT NULL,
    to_state            VARCHAR(32)     NOT NULL,
    reason              VARCHAR(255),
    triggered_by        VARCHAR(64),
    trigger_source      VARCHAR(64),
    transitioned_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coordination_state_log_coordination
    ON coordination_state_log (coordination_id);
CREATE INDEX IF NOT EXISTS idx_coordination_state_log_time
    ON coordination_state_log (transitioned_at);

-- ==========================================================================
-- 7. APPROVALS
-- Owner: approval-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS approvals (
    approval_id     UUID            PRIMARY KEY,
    coordination_id UUID,
    user_id         UUID            NOT NULL,
    approval_type   VARCHAR(16)     NOT NULL,
    decision        VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
    event_details   JSONB,
    expires_at      TIMESTAMPTZ     NOT NULL,
    decided_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approval_type     CHECK (approval_type IN ('PERSONAL', 'COLLABORATIVE')),
    CONSTRAINT chk_approval_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_approvals_user_id
    ON approvals (user_id);
CREATE INDEX IF NOT EXISTS idx_approvals_coordination
    ON approvals (coordination_id);
CREATE INDEX IF NOT EXISTS idx_approvals_expires_at
    ON approvals (expires_at) WHERE decision = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_approvals_pending
    ON approvals (user_id, decision) WHERE decision = 'PENDING';

-- ==========================================================================
-- 8. AUDIT LOGS
-- Owner: monitoring-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS agent_activities (
    log_id          UUID            PRIMARY KEY,
    agent_id        UUID            NOT NULL,
    correlation_id  UUID,
    coordination_id UUID,
    event_type      VARCHAR(64)     NOT NULL,
    description     TEXT            NOT NULL,
    level           VARCHAR(16)     NOT NULL,
    occurred_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_activities_agent_id
    ON agent_activities (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_activities_correlation_id
    ON agent_activities (correlation_id);
CREATE INDEX IF NOT EXISTS idx_agent_activities_coordination_id
    ON agent_activities (coordination_id);
CREATE INDEX IF NOT EXISTS idx_agent_activities_occurred_at
    ON agent_activities (occurred_at);

-- ==========================================================================
-- 9. EVENT PROPOSALS
-- Owner: agent-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS event_proposals (
    id              UUID            PRIMARY KEY,
    agent_id        UUID            NOT NULL,
    approval_id     UUID,
    title           VARCHAR(500)    NOT NULL,
    start_time      TIMESTAMPTZ     NOT NULL,
    end_time        TIMESTAMPTZ     NOT NULL,
    status          VARCHAR(50)     NOT NULL DEFAULT 'INITIATED',
    event_id        VARCHAR(255),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_proposals_approval
    ON event_proposals (approval_id);
CREATE INDEX IF NOT EXISTS idx_event_proposals_agent
    ON event_proposals (agent_id);
CREATE INDEX IF NOT EXISTS idx_event_proposals_status
    ON event_proposals (status);

-- ==========================================================================
-- 10. WORKSPACE INSTALLATIONS
-- Owner: user-module (Slack bot tokens)
-- ==========================================================================
CREATE TABLE IF NOT EXISTS workspace_installations (
    workspace_id        VARCHAR(50)     PRIMARY KEY,
    bot_token           VARCHAR(255)    NOT NULL,
    installer_user_id   VARCHAR(50),
    installed_at        TIMESTAMPTZ     NOT NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ==========================================================================
-- 11. REVOKED TOKENS  (JWT blacklist)
-- Owner: security-module
-- ==========================================================================
CREATE TABLE IF NOT EXISTS revoked_tokens (
    jti         VARCHAR(64)     PRIMARY KEY,
    user_id     UUID            NOT NULL,
    revoked_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires
    ON revoked_tokens (expires_at);
