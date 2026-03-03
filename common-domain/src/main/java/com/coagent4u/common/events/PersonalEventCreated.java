package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.UserId;

/**
 * Published when an agent successfully creates a personal calendar event on
 * behalf of a user.
 */
public record PersonalEventCreated(
        AgentId agentId,
        UserId userId,
        EventId calendarEventId,
        TimeSlot timeSlot,
        String title,
        Instant occurredAt) implements DomainEvent {
    public PersonalEventCreated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(calendarEventId, "calendarEventId must not be null");
        Objects.requireNonNull(timeSlot, "timeSlot must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static PersonalEventCreated of(AgentId agentId, UserId userId, EventId calendarEventId,
            TimeSlot timeSlot, String title) {
        return new PersonalEventCreated(agentId, userId, calendarEventId, timeSlot, title, Instant.now());
    }
}
