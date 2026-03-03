package com.coagent4u.persistence.agent;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentJpaRepository extends JpaRepository<AgentJpaEntity, UUID> {
    Optional<AgentJpaEntity> findByUserId(UUID userId);
}
