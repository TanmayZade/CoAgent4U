-- V13: Expanded Slack metadata for identities
-- Captured during sign-in to improve UI/UX

ALTER TABLE slack_identities 
    ADD COLUMN IF NOT EXISTS workspace_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS workspace_domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS avatar_url        TEXT;
