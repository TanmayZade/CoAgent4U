package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Published when a personal agent is provisioned for a newly registered user.
 */
public record AgentProvisioned(
        AgentId agentId,
        UserId userId,
        Instant occurredAt) implements DomainEvent {
    public AgentProvisioned {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static AgentProvisioned of(AgentId agentId, UserId userId) {
        return new AgentProvisioned(agentId, userId, Instant.now());
    }
}
