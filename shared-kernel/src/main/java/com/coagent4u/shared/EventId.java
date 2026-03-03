package com.coagent4u.shared;

import java.util.Objects;

/** Strongly-typed external calendar event identifier. */
public record EventId(String value) {
    public EventId {
        Objects.requireNonNull(value, "EventId value must not be null");
        if (value.isBlank())
            throw new IllegalArgumentException("EventId value must not be blank");
    }

    public static EventId of(String value) {
        return new EventId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
