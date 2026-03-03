package com.coagent4u.coordination.port.out;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Outbound port — retrieves profile information about an agent.
 * Implemented by agent-module's {@code AgentProfilePortImpl}.
 */
public interface AgentProfilePort {
    AgentProfile getProfile(AgentId agentId);

    /** Simple profile descriptor. */
    record AgentProfile(AgentId agentId, UserId userId, String displayName, String timezone) {
    }
}
