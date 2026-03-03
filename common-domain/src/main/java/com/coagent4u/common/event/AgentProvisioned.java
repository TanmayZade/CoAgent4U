package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a personal agent is provisioned for a newly registered user.
 */
public record AgentProvisioned(
        UUID eventId,
        Instant occurredAt,
        AgentId agentId,
        UserId userId
) implements DomainEvent {

    public AgentProvisioned(AgentId agentId, UserId userId) {
        this(UUID.randomUUID(), Instant.now(), agentId, userId);
    }
}
