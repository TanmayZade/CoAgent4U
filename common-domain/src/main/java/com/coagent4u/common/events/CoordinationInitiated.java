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
 * (Requester's log) Fired when the requester's agent reaches out to the
 * target user's agent to initiate a coordination flow.
 */
public record CoordinationInitiated(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        UserId targetUserId,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public CoordinationInitiated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(targetUserId, "targetUserId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationInitiated of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId,
            UserId targetUserId) {
        return new CoordinationInitiated(agentId, userId, correlationId, coordinationId, targetUserId, Instant.now());
    }
}
