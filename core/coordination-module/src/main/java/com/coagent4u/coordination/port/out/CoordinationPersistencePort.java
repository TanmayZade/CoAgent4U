package com.coagent4u.coordination.port.out;

import java.util.Optional;

import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationStateLogEntry;
import com.coagent4u.shared.CoordinationId;

/**
 * Outbound port — persistence operations for the Coordination aggregate.
 * Implemented in the persistence module (CoordinationPersistenceAdapter).
 */
public interface CoordinationPersistencePort {
    Coordination save(Coordination coordination);

    Optional<Coordination> findById(CoordinationId coordinationId);

    java.util.List<Coordination> findByAgentId(com.coagent4u.shared.AgentId agentId, int offset, int limit);

    long countByAgentId(com.coagent4u.shared.AgentId agentId);

    java.util.List<Coordination> findRecentByAgentId(com.coagent4u.shared.AgentId agentId, int limit);

    void appendStateLog(CoordinationStateLogEntry entry);
}
