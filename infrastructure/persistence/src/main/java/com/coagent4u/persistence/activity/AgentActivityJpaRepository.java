package com.coagent4u.persistence.activity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentActivityJpaRepository extends JpaRepository<AgentActivityJpaEntity, UUID> {
    Page<AgentActivityJpaEntity> findByAgentIdOrderByOccurredAtDesc(UUID agentId, Pageable pageable);

    Page<AgentActivityJpaEntity> findByAgentIdAndEventTypeOrderByOccurredAtDesc(
            UUID agentId, String eventType, Pageable pageable);

    long countByAgentId(UUID agentId);

    long countByAgentIdAndEventType(UUID agentId, String eventType);

    List<AgentActivityJpaEntity> findByAgentIdOrderByOccurredAtDesc(UUID agentId);
}
