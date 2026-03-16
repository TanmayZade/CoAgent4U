package com.coagent4u.persistence.activity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

import com.coagent4u.persistence.agent.AgentJpaEntity;
import com.coagent4u.persistence.agent.AgentJpaRepository;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AgentActivityEntry;
import com.coagent4u.user.port.out.AgentActivityQueryPort;

/**
 * Persistence adapter implementing AgentActivityQueryPort.
 * Resolves UserId → AgentId before querying agent_activities.
 */
@Component
public class AgentActivityQueryAdapter implements AgentActivityQueryPort {

    private final AgentActivityJpaRepository repository;
    private final AgentJpaRepository agentRepository;

    public AgentActivityQueryAdapter(AgentActivityJpaRepository repository, AgentJpaRepository agentRepository) {
        this.repository = repository;
        this.agentRepository = agentRepository;
    }

    @Override
    public List<AgentActivityEntry> findByUserId(
            UserId userId, String eventTypeFilter, String levelFilter, Instant startDate, Instant endDate, int offset, int limit) {
        UUID agentId = resolveAgentId(userId);
        if (agentId == null) return Collections.emptyList();

        int page = offset / Math.max(limit, 1);
        PageRequest pageRequest = PageRequest.of(page, limit);

        Page<AgentActivityJpaEntity> result = repository.findFiltered(
                agentId, eventTypeFilter, levelFilter, startDate, endDate, pageRequest);

        return result.getContent().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long countByUserId(UserId userId, String eventTypeFilter, String levelFilter, Instant startDate, Instant endDate) {
        UUID agentId = resolveAgentId(userId);
        if (agentId == null) return 0L;

        return repository.countFiltered(agentId, eventTypeFilter, levelFilter, startDate, endDate);
    }

    @Override
    public List<AgentActivityEntry> findAllByUserId(UserId userId) {
        UUID agentId = resolveAgentId(userId);
        if (agentId == null) return Collections.emptyList();

        return repository.findByAgentIdOrderByOccurredAtDesc(agentId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Resolve a user's agent ID. Returns null if no agent exists for this user. */
    private UUID resolveAgentId(UserId userId) {
        Optional<AgentJpaEntity> agent = agentRepository.findByUserId(userId.value());
        return agent.map(AgentJpaEntity::getAgentId).orElse(null);
    }

    private AgentActivityEntry toDto(AgentActivityJpaEntity entity) {
        return new AgentActivityEntry(
                entity.getLogId(),
                entity.getCorrelationId(),
                entity.getCoordinationId(),
                entity.getEventType(),
                entity.getDescription(),
                entity.getLevel(),
                entity.getOccurredAt());
    }
}
