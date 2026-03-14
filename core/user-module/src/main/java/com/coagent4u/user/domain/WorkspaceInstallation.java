package com.coagent4u.user.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Value object representing a Slack workspace installation.
 */
public record WorkspaceInstallation(
        WorkspaceId workspaceId,
        String botToken,
        UserId installerUserId,
        Instant installedAt) {
    
    public WorkspaceInstallation {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(botToken, "botToken must not be null");
        Objects.requireNonNull(installedAt, "installedAt must not be null");
    }

    public static WorkspaceInstallation of(WorkspaceId workspaceId, String botToken, UserId installerUserId) {
        return new WorkspaceInstallation(workspaceId, botToken, installerUserId, Instant.now());
    }
}
