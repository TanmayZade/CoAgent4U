-- V15: Add active column to workspace_installations
ALTER TABLE workspace_installations ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
