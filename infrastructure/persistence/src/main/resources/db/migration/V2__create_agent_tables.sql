-- V2: Agent module tables
-- Owner: agent-module (AgentPersistencePort)

CREATE TABLE IF NOT EXISTS agents (
    agent_id        UUID PRIMARY KEY,
    user_id         UUID NOT NULL,        -- NO FK to users (cross-module boundary)
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_agent_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
