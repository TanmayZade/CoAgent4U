package com.coagent4u.persistence.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workspace_installations")
public class WorkspaceInstallationJpaEntity {

    @Id
    @Column(name = "workspace_id", length = 50, nullable = false)
    private String workspaceId;

    @Column(name = "bot_token", length = 255, nullable = false)
    private String botToken;

    @Column(name = "installer_user_id")
    private UUID installerUserId;

    @Column(name = "installed_at", nullable = false, updatable = false)
    private Instant installedAt = Instant.now();

    // Default constructor for JPA
    protected WorkspaceInstallationJpaEntity() {
    }

    public WorkspaceInstallationJpaEntity(String workspaceId, String botToken, UUID installerUserId) {
        this.workspaceId = workspaceId;
        this.botToken = botToken;
        this.installerUserId = installerUserId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public UUID getInstallerUserId() {
        return installerUserId;
    }

    public void setInstallerUserId(UUID installerUserId) {
        this.installerUserId = installerUserId;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(Instant installedAt) {
        this.installedAt = installedAt;
    }
}
