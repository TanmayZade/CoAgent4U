package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * Fired when the agent views the user's schedule.
 */
public record ScheduleViewed(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        int eventCount,
        Instant occurredAt) implements CorrelationAwareEvent, UserAwareEvent {
    public ScheduleViewed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ScheduleViewed of(AgentId agentId, UserId userId, CorrelationId correlationId, int eventCount) {
        return new ScheduleViewed(agentId, userId, correlationId, eventCount, Instant.now());
    }
}
