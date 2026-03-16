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
 * (Invitee's log) Fired when the invitee's agent receives proposed slots
 * from the requester.
 */
public record SlotsReceived(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        int slotCount,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public SlotsReceived {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static SlotsReceived of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId, int slotCount) {
        return new SlotsReceived(agentId, userId, correlationId, coordinationId, slotCount, Instant.now());
    }
}
