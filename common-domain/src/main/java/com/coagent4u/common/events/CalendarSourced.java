package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired when the agent successfully pulls its user's calendar for
 * availability comparison during coordination.
 */
public record CalendarSourced(
        AgentId agentId,
        UserId userId,
        int eventCount,
        Instant occurredAt) implements UserAwareEvent {
    public CalendarSourced {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CalendarSourced of(AgentId agentId, UserId userId, int eventCount) {
        return new CalendarSourced(agentId, userId, eventCount, Instant.now());
    }
}
