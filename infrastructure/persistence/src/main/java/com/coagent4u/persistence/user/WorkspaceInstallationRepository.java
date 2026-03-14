package com.coagent4u.persistence.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceInstallationRepository extends CrudRepository<WorkspaceInstallationJpaEntity, String> {
}
