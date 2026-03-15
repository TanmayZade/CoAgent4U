package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

/**
 * (Invitee's log) Fired when the invitee's agent receives an incoming
 * meeting request from another agent.
 */
public record CoordinationRequestReceived(
        AgentId agentId,
        UserId userId,
        CoordinationId coordinationId,
        UserId requesterUserId,
        Instant occurredAt) implements UserAwareEvent {
    public CoordinationRequestReceived {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(requesterUserId, "requesterUserId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationRequestReceived of(AgentId agentId, UserId userId, CoordinationId coordinationId,
            UserId requesterUserId) {
        return new CoordinationRequestReceived(agentId, userId, coordinationId, requesterUserId, Instant.now());
    }
}
