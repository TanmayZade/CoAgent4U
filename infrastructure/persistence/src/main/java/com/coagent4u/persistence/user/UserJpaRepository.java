package com.coagent4u.persistence.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserJpaEntity> findBySlackIdentity_SlackUserIdAndSlackIdentity_WorkspaceId(
            String slackUserId, String workspaceId);
}
