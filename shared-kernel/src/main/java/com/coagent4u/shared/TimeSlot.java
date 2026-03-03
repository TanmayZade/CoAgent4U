package com.coagent4u.shared;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a concrete time slot: a start time combined with a duration.
 * Used for meeting proposals and calendar event creation.
 */
public record TimeSlot(Instant start, java.time.Duration duration) {

    public TimeSlot {
        Objects.requireNonNull(start, "TimeSlot start must not be null");
        Objects.requireNonNull(duration, "TimeSlot duration must not be null");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("TimeSlot duration must be positive");
        }
    }

    public Instant end() {
        return start.plus(duration);
    }

    public TimeRange toTimeRange() {
        return new TimeRange(start, end());
    }
}
