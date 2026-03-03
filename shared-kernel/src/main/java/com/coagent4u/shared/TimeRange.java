package com.coagent4u.shared;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a time range with a start (inclusive) and end (exclusive) instant.
 * Used for calendar queries and availability checks.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(start, "TimeRange start must not be null");
        Objects.requireNonNull(end, "TimeRange end must not be null");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("TimeRange end must be after start");
        }
    }

    public boolean overlaps(TimeRange other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public java.time.Duration duration() {
        return java.time.Duration.between(start, end);
    }
}
