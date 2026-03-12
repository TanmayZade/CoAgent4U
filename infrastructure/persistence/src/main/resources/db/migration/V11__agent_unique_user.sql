-- V11: Enforce 1 agent per user at the database level.
-- The application logic is already idempotent (check-then-insert),
-- but this constraint acts as a safety net for race conditions.
CREATE UNIQUE INDEX IF NOT EXISTS idx_agents_user_id ON agents(user_id);
