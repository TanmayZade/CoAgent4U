package com.coagent4u.shared;

import java.util.Objects;

/**
 * A calendar event with a title and time slot.
 * Used for richer schedule display compared to bare TimeSlot.
 */
public record CalendarEvent(EventId eventId, String title, TimeSlot slot) {
    public CalendarEvent {
        Objects.requireNonNull(eventId, "CalendarEvent eventId must not be null");
        Objects.requireNonNull(slot, "CalendarEvent slot must not be null");
        if (title == null || title.isBlank()) {
            title = "(No title)";
        }
    }
}
