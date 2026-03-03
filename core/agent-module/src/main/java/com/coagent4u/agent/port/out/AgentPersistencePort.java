package com.coagent4u.agent.port.out;

import java.util.Optional;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Outbound port — persistence for the Agent aggregate.
 * Implemented in persistence-module (AgentPersistenceAdapter).
 */
public interface AgentPersistencePort {
    Agent save(Agent agent);

    Optional<Agent> findById(AgentId agentId);

    Optional<Agent> findByUserId(UserId userId);
}
