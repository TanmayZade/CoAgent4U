-- V17: Remove Slack DM Channel ID storage
-- As per user request, we are removing all logic related to DM channel IDs.

ALTER TABLE slack_identities
    DROP COLUMN IF EXISTS dm_channel_id;
