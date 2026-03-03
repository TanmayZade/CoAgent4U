package com.coagent4u.persistence.agent;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

@Component
public class AgentPersistenceAdapter implements AgentPersistencePort {

    private final AgentJpaRepository repository;

    public AgentPersistenceAdapter(AgentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Agent save(Agent agent) {
        AgentJpaEntity entity = AgentMapper.toJpa(agent);
        repository.save(entity);
        return agent;
    }

    @Override
    public Optional<Agent> findById(AgentId agentId) {
        return repository.findById(agentId.value()).map(AgentMapper::toDomain);
    }

    @Override
    public Optional<Agent> findByUserId(UserId userId) {
        return repository.findByUserId(userId.value())
                .stream().findFirst().map(AgentMapper::toDomain);
    }
}
