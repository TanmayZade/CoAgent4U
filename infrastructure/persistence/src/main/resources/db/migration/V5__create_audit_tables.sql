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
