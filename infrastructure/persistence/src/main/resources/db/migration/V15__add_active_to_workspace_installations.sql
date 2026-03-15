-- V15: Add active column to workspace_installations
ALTER TABLE workspace_installations ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
