package com.coagent4u.user.domain;

import java.time.Instant;
import com.coagent4u.shared.WorkspaceId;

/**
 * Represents a Slack App installation in a specific workspace.
 * Stores the bot token required to interact with that workspace.
 */
public record WorkspaceInstallation(
        WorkspaceId workspaceId,
        String botToken,
        String installerUserId,
        Instant installedAt,
        boolean active
) {
    public WorkspaceInstallation withActive(boolean active) {
        return new WorkspaceInstallation(workspaceId, botToken, installerUserId, installedAt, active);
    }
}
