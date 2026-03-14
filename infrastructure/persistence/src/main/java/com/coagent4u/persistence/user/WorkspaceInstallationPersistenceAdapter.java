package com.coagent4u.persistence.user;

import com.coagent4u.user.domain.WorkspaceInstallation;
import com.coagent4u.user.port.out.WorkspaceInstallationPersistencePort;
import com.coagent4u.shared.WorkspaceId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkspaceInstallationPersistenceAdapter implements WorkspaceInstallationPersistencePort {

    private final WorkspaceInstallationRepository repository;

    @Override
    public void save(WorkspaceInstallation installation) {
        WorkspaceInstallationJpaEntity entity = WorkspaceInstallationJpaEntity.builder()
                .workspaceId(installation.workspaceId().value())
                .botToken(installation.botToken())
                .installerUserId(installation.installerUserId())
                .installedAt(installation.installedAt())
                .active(installation.active())
                .build();
        repository.save(entity);
    }

    @Override
    public Optional<WorkspaceInstallation> findByWorkspaceId(WorkspaceId workspaceId) {
        return repository.findById(workspaceId.value())
                .map(entity -> new WorkspaceInstallation(
                        new WorkspaceId(entity.getWorkspaceId()),
                        entity.getBotToken(),
                        entity.getInstallerUserId(),
                        entity.getInstalledAt(),
                        entity.isActive()
                ));
    }
}
