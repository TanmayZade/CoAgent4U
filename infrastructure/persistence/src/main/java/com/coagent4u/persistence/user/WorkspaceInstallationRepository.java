package com.coagent4u.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceInstallationRepository extends JpaRepository<WorkspaceInstallationJpaEntity, String> {
}
