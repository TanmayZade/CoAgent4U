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
