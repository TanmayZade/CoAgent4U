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
 * (Invitee's log) Fired when the invitee's agent receives an incoming
 * meeting request from another agent.
 */
public record CoordinationRequestReceived(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        UserId requesterUserId,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public CoordinationRequestReceived {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(requesterUserId, "requesterUserId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationRequestReceived of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId,
            UserId requesterUserId) {
        return new CoordinationRequestReceived(agentId, userId, correlationId, coordinationId, requesterUserId, Instant.now());
    }
}
