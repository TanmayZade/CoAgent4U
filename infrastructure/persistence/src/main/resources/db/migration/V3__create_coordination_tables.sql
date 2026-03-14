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
