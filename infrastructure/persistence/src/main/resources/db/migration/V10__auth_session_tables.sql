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
