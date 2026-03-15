package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

/**
 * (Invitee's log) Fired when the invitee's agent receives proposed slots
 * and presents them to its user via Slack.
 */
public record SlotsReceived(
        AgentId agentId,
        UserId userId,
        CoordinationId coordinationId,
        int slotCount,
        Instant occurredAt) implements UserAwareEvent {
    public SlotsReceived {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static SlotsReceived of(AgentId agentId, UserId userId, CoordinationId coordinationId, int slotCount) {
        return new SlotsReceived(agentId, userId, coordinationId, slotCount, Instant.now());
    }
}
