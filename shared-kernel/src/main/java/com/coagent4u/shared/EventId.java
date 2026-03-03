package com.coagent4u.shared;

import java.util.Objects;

/**
 * Strongly-typed identifier for a calendar event in an external provider (e.g.
 * Google Calendar).
 */
public record EventId(String value) {

    public EventId {
        Objects.requireNonNull(value, "EventId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EventId value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
