-- V16: Add explicit Slack DM Channel ID storage
-- Captured dynamically via conversations.open during the OAuth signin flow
-- Ensures the dashboard "Chat with Agent" button has an exact channel to link to

ALTER TABLE slack_identities
    ADD COLUMN IF NOT EXISTS dm_channel_id VARCHAR(50);
