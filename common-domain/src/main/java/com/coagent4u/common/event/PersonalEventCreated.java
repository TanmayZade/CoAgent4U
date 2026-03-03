package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a personal calendar event is successfully created in Google Calendar.
 */
public record PersonalEventCreated(
        UUID eventId,
        Instant occurredAt,
        UserId userId,
        EventId calendarEventId,
        String title
) implements DomainEvent {

    public PersonalEventCreated(UserId userId, EventId calendarEventId, String title) {
        this(UUID.randomUUID(), Instant.now(), userId, calendarEventId, title);
    }
}
