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
