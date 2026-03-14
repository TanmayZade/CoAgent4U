-- V14: Create workspace installations for Slack bot tokens

CREATE TABLE workspace_installations (
    workspace_id VARCHAR(50) PRIMARY KEY,
    bot_token VARCHAR(255) NOT NULL,
    installer_user_id UUID,
    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_workspace_installations_workspace_id ON workspace_installations(workspace_id);
