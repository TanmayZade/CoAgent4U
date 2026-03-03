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

    void appendStateLog(CoordinationStateLogEntry entry);
}
