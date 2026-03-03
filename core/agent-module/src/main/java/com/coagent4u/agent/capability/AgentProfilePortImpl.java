package com.coagent4u.agent.capability;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Implements {@link AgentProfilePort} (coordination-module's outbound
 * interface)
 * by querying agent and user data from persistence.
 */
public class AgentProfilePortImpl implements AgentProfilePort {

    private final AgentPersistencePort agentPersistence;
    private final UserQueryPort userQuery;

    public AgentProfilePortImpl(AgentPersistencePort agentPersistence,
            UserQueryPort userQuery) {
        this.agentPersistence = agentPersistence;
        this.userQuery = userQuery;
    }

    @Override
    public AgentProfile getProfile(AgentId agentId) {
        var agent = agentPersistence.findById(agentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Agent not found: " + agentId));

        UserId userId = agent.getUserId();
        String displayName = userQuery.findById(userId)
                .map(u -> u.getUsername())
                .orElse("unknown");

        // Timezone defaults to UTC until Phase 2 adds user preferences
        return new AgentProfile(agentId, userId, displayName, "UTC");
    }
}
