package com.coagent4u.user.port.out;

import java.util.Optional;

import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.WorkspaceInstallation;

/**
 * Outbound port — saves and retrieves Slack workspace bot tokens.
 */
public interface WorkspaceInstallationPersistencePort {

    /**
     * Finds a workspace installation by its ID.
     */
    Optional<WorkspaceInstallation> findByWorkspaceId(WorkspaceId workspaceId);

    /**
     * Saves a new or updated workspace installation.
     */
    WorkspaceInstallation save(WorkspaceInstallation installation);
}
