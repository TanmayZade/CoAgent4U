package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired when VIEW_SCHEDULE is executed and calendar data is sent to user.
 */
public record ScheduleViewed(
        AgentId agentId,
        UserId userId,
        int eventCount,
        Instant occurredAt) implements UserAwareEvent {
    public ScheduleViewed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ScheduleViewed of(AgentId agentId, UserId userId, int eventCount) {
        return new ScheduleViewed(agentId, userId, eventCount, Instant.now());
    }
}
