package com.coagent4u.coordination.port.out;

import java.util.List;

import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeRange;

/**
 * Outbound port — retrieves an agent's free/busy availability blocks.
 * Implemented by agent-module's {@code AgentAvailabilityPortImpl}.
 */
public interface AgentAvailabilityPort {
    List<AvailabilityBlock> getAvailability(AgentId agentId, TimeRange range);
}
