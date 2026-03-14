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
