package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * (Requester's log) Fired when the requester's agent sends available
 * time options to the invitee's agent.
 */
public record SlotsProposed(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        int slotCount,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public SlotsProposed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static SlotsProposed of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId, int slotCount) {
        return new SlotsProposed(agentId, userId, correlationId, coordinationId, slotCount, Instant.now());
    }
}
