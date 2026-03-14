package com.coagent4u.persistence.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.WorkspaceInstallation;
import com.coagent4u.user.port.out.WorkspaceInstallationPersistencePort;

@Component
public class WorkspaceInstallationPersistenceAdapter implements WorkspaceInstallationPersistencePort {

    private final WorkspaceInstallationRepository repository;

    public WorkspaceInstallationPersistenceAdapter(WorkspaceInstallationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkspaceInstallation> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findById(workspaceId.value())
                .map(this::mapToDomain);
    }

    @Override
    public WorkspaceInstallation save(WorkspaceInstallation installation) {
        WorkspaceInstallationJpaEntity entity = repository.findById(installation.workspaceId().value())
                .orElseGet(() -> new WorkspaceInstallationJpaEntity());

        entity.setWorkspaceId(installation.workspaceId().value());
        entity.setBotToken(installation.botToken());
        if (installation.installerUserId() != null) {
            entity.setInstallerUserId(installation.installerUserId().value());
        }
        // installedAt is set on creation and shouldn't be overridden

        WorkspaceInstallationJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    private WorkspaceInstallation mapToDomain(WorkspaceInstallationJpaEntity entity) {
        return new WorkspaceInstallation(
                new WorkspaceId(entity.getWorkspaceId()),
                entity.getBotToken(),
                entity.getInstallerUserId() != null ? new UserId(entity.getInstallerUserId()) : null,
                entity.getInstalledAt());
    }
}
