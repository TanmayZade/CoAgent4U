package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.UserId;

/**
 * Fired when a calendar event is successfully created on the user's calendar at the end of a coordination.
 */
public record CalendarEventCreated(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        CoordinationId coordinationId,
        EventId calendarEventId,
        Instant occurredAt) implements CorrelationAwareEvent, CoordinationAwareEvent, UserAwareEvent {
    public CalendarEventCreated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(calendarEventId, "calendarEventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CalendarEventCreated of(AgentId agentId, UserId userId, CorrelationId correlationId, CoordinationId coordinationId, EventId calendarEventId) {
        return new CalendarEventCreated(agentId, userId, correlationId, coordinationId, calendarEventId, Instant.now());
    }
}
