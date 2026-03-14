package com.coagent4u.user.port.out;

import com.coagent4u.user.domain.WorkspaceInstallation;
import com.coagent4u.shared.WorkspaceId;

import java.util.Optional;

/**
 * Outbound port for persisting and retrieving Slack workspace installations.
 */
public interface WorkspaceInstallationPersistencePort {
    void save(WorkspaceInstallation installation);
    Optional<WorkspaceInstallation> findByWorkspaceId(WorkspaceId workspaceId);
}
