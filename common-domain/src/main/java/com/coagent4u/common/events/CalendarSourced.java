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
 * Fired when the agent successfully pulls its user's calendar for
 * availability comparison during coordination.
 */
public record CalendarSourced(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        int eventCount,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public CalendarSourced {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CalendarSourced of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId, int eventCount) {
        return new CalendarSourced(agentId, userId, correlationId, coordinationId, eventCount, Instant.now());
    }
}
