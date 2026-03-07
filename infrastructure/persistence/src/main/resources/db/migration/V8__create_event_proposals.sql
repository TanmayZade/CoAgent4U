-- Event proposals table for tracking personal event creation workflow
CREATE TABLE event_proposals (
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

CREATE INDEX idx_event_proposals_approval ON event_proposals(approval_id);
CREATE INDEX idx_event_proposals_agent ON event_proposals(agent_id);
CREATE INDEX idx_event_proposals_status ON event_proposals(status);
