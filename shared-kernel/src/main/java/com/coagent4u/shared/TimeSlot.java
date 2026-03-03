package com.coagent4u.shared;

import java.time.Instant;
import java.util.Objects;

/**
 * A specific time slot with a start and end instant.
 * Used for calendar event slots and availability blocks.
 */
public record TimeSlot(Instant start, Instant end) {
    public TimeSlot {
        Objects.requireNonNull(start, "TimeSlot start must not be null");
        Objects.requireNonNull(end, "TimeSlot end must not be null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("TimeSlot start must be before end");
        }
    }

    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public java.time.Duration duration() {
        return java.time.Duration.between(start, end);
    }
}
