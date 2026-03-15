CREATE TABLE IF NOT EXISTS workspace_installations (
    workspace_id VARCHAR(50) PRIMARY KEY,
    bot_token VARCHAR(255) NOT NULL,
    installer_user_id VARCHAR(50),
    installed_at TIMESTAMP NOT NULL
);
