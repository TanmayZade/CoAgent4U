-- V10: Auth and session support tables
-- Token blacklist for JWT revocation (logout support)
CREATE TABLE IF NOT EXISTS revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    user_id     UUID NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_tokens(expires_at);

-- Add google_account_id to service_connections for Google Calendar
ALTER TABLE service_connections
    ADD COLUMN IF NOT EXISTS google_account_id VARCHAR(128);

-- Add disconnected_at timestamp to service_connections
ALTER TABLE service_connections
    ADD COLUMN IF NOT EXISTS disconnected_at TIMESTAMPTZ;
-- V11: Enforce 1 agent per user at the database level.
-- The application logic is already idempotent (check-then-insert),
-- but this constraint acts as a safety net for race conditions.
CREATE UNIQUE INDEX IF NOT EXISTS idx_agents_user_id ON agents(user_id);
-- V12: Add metadata column to coordinations table
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS metadata_json JSONB;
-- V13: Expanded Slack metadata for identities
-- Captured during sign-in to improve UI/UX

ALTER TABLE slack_identities 
    ADD COLUMN IF NOT EXISTS workspace_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS workspace_domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS avatar_url        TEXT;
CREATE TABLE IF NOT EXISTS workspace_installations (
    workspace_id VARCHAR(50) PRIMARY KEY,
    bot_token VARCHAR(255) NOT NULL,
    installer_user_id VARCHAR(50),
    installed_at TIMESTAMP NOT NULL
);
-- V15: Add active column to workspace_installations
ALTER TABLE workspace_installations ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
-- V16: Add explicit Slack DM Channel ID storage
-- Captured dynamically via conversations.open during the OAuth signin flow
-- Ensures the dashboard "Chat with Agent" button has an exact channel to link to

ALTER TABLE slack_identities
    ADD COLUMN IF NOT EXISTS dm_channel_id VARCHAR(50);
-- V17: Remove Slack DM Channel ID storage
-- As per user request, we are removing all logic related to DM channel IDs.

ALTER TABLE slack_identities
    DROP COLUMN IF EXISTS dm_channel_id;
-- V1: User module tables
-- Owner: user-module (UserPersistencePort)

CREATE TABLE IF NOT EXISTS users (
    user_id         UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    preferences     JSONB
);

CREATE TABLE IF NOT EXISTS slack_identities (
    slack_user_id   VARCHAR(64) PRIMARY KEY,
    workspace_id    VARCHAR(64) NOT NULL,
    display_name    VARCHAR(255),
    linked_user_id  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS service_connections (
    connection_id       UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    service_type        VARCHAR(32) NOT NULL,
    encrypted_token     TEXT NOT NULL,
    token_expires_at    TIMESTAMPTZ,
    status              VARCHAR(16) NOT NULL DEFAULT 'CONNECTED',
    connected_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_type CHECK (service_type IN ('GOOGLE_CALENDAR')),
    CONSTRAINT chk_connection_status CHECK (status IN ('CONNECTED', 'EXPIRED', 'REVOKED'))
);
-- V2: Agent module tables
-- Owner: agent-module (AgentPersistencePort)

CREATE TABLE IF NOT EXISTS agents (
    agent_id        UUID PRIMARY KEY,
    user_id         UUID NOT NULL,        -- NO FK to users (cross-module boundary)
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
-- V3: Coordination module tables
-- Owner: coordination-module (CoordinationPersistencePort)

CREATE TABLE IF NOT EXISTS coordinations (
    coordination_id     UUID PRIMARY KEY,
    requester_agent_id  UUID NOT NULL,       -- NO FK to agents (cross-module boundary)
    invitee_agent_id    UUID NOT NULL,       -- NO FK to agents (cross-module boundary)
    state               VARCHAR(32) NOT NULL,
    proposal            JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    CONSTRAINT chk_coordination_state CHECK (state IN (
        'INITIATED',
        'CHECKING_AVAILABILITY',
        'AVAILABILITY_RECEIVED',
        'PROPOSAL_GENERATED',
        'AWAITING_REQUESTER_APPROVAL',
        'AWAITING_INVITEE_APPROVAL',
        'APPROVED_BY_REQUESTER',
        'APPROVED_BY_INVITEE',
        'APPROVED_BY_BOTH',
        'CREATING_EVENT_A',
        'CREATING_EVENT_B',
        'COMPLETED',
        'REJECTED',
        'FAILED'
    ))
);

CREATE TABLE IF NOT EXISTS coordination_state_log (
    log_id              UUID PRIMARY KEY,
    coordination_id     UUID NOT NULL REFERENCES coordinations(coordination_id),
    from_state          VARCHAR(32) NOT NULL,
    to_state            VARCHAR(32) NOT NULL,
    triggered_by        VARCHAR(64),
    trigger_source      VARCHAR(64),
    transitioned_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- V4: Approval module tables
-- Owner: approval-module (ApprovalPersistencePort)

CREATE TABLE IF NOT EXISTS approvals (
    approval_id         UUID PRIMARY KEY,
    coordination_id     UUID,                    -- nullable for PERSONAL type; NO FK (cross-module)
    user_id             UUID NOT NULL,           -- NO FK to users (cross-module boundary)
    approval_type       VARCHAR(16) NOT NULL,
    decision            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    event_details       JSONB,
    expires_at          TIMESTAMPTZ NOT NULL,
    decided_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approval_type CHECK (approval_type IN ('PERSONAL', 'COLLABORATIVE')),
    CONSTRAINT chk_approval_decision CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);
-- V5: Monitoring / audit tables
-- Owner: monitoring module (AuditPersistencePort)

CREATE TABLE IF NOT EXISTS audit_logs (
    log_id          UUID PRIMARY KEY,
    user_id         UUID,
    event_type      VARCHAR(64) NOT NULL,
    payload         JSONB,
    correlation_id  VARCHAR(64),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- V6: Performance indexes across all module tables

-- user-module indexes
CREATE INDEX IF NOT EXISTS idx_slack_identities_user_id ON slack_identities(linked_user_id);
CREATE INDEX IF NOT EXISTS idx_service_connections_user_id ON service_connections(user_id);
CREATE INDEX IF NOT EXISTS idx_service_connections_status ON service_connections(user_id, service_type, status);

-- agent-module indexes
CREATE INDEX IF NOT EXISTS idx_agents_user_id ON agents(user_id);

-- coordination-module indexes
CREATE INDEX IF NOT EXISTS idx_coordinations_requester ON coordinations(requester_agent_id);
CREATE INDEX IF NOT EXISTS idx_coordinations_invitee ON coordinations(invitee_agent_id);
CREATE INDEX IF NOT EXISTS idx_coordinations_state ON coordinations(state);
CREATE INDEX IF NOT EXISTS idx_coordination_state_log_coordination ON coordination_state_log(coordination_id);
CREATE INDEX IF NOT EXISTS idx_coordination_state_log_time ON coordination_state_log(transitioned_at);

-- approval-module indexes
CREATE INDEX IF NOT EXISTS idx_approvals_user_id ON approvals(user_id);
CREATE INDEX IF NOT EXISTS idx_approvals_coordination ON approvals(coordination_id);
CREATE INDEX IF NOT EXISTS idx_approvals_expires_at ON approvals(expires_at) WHERE decision = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_approvals_pending ON approvals(user_id, decision) WHERE decision = 'PENDING';

-- audit indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_occurred_at ON audit_logs(occurred_at);
-- V7: Align schema with Phase 1 domain model
-- Patches V1-V6 tables to match aggregate root fields
-- IMPORTANT: No CHECK constraints that duplicate domain state machine logic

-- 1. Users: add missing columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
    ON users(username) WHERE username IS NOT NULL;

-- 2. Service connections: add refresh token column
ALTER TABLE service_connections
    ADD COLUMN IF NOT EXISTS encrypted_refresh_token TEXT;

-- 3. Coordinations: DROP the CHECK constraint entirely.
--    Domain CoordinationStateMachine is the single source of truth.
--    VARCHAR(32) accepts any string; the domain validates before persisting.
ALTER TABLE coordinations DROP CONSTRAINT IF EXISTS chk_coordination_state;

-- 4. Coordinations: add reason column
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS reason VARCHAR(255);

-- 5. Coordination state log: add reason column
ALTER TABLE coordination_state_log ADD COLUMN IF NOT EXISTS reason VARCHAR(255);
-- Event proposals table for tracking personal event creation workflow
CREATE TABLE IF NOT EXISTS event_proposals (
    id              UUID PRIMARY KEY,
    agent_id        UUID NOT NULL,
    approval_id     UUID,
    title           VARCHAR(500) NOT NULL,
    start_time      TIMESTAMPTZ NOT NULL,
    end_time        TIMESTAMPTZ NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    event_id        VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_proposals_approval ON event_proposals(approval_id);
CREATE INDEX IF NOT EXISTS idx_event_proposals_agent ON event_proposals(agent_id);
CREATE INDEX IF NOT EXISTS idx_event_proposals_status ON event_proposals(status);
-- V9: Add slot columns to coordinations table for slot selection workflow
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS available_slots_json JSONB;
ALTER TABLE coordinations ADD COLUMN IF NOT EXISTS selected_slot_json JSONB;
