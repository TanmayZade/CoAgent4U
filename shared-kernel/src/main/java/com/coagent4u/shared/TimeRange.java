package com.coagent4u.shared;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A date range (inclusive on both ends) for calendar searches.
 */
public record TimeRange(LocalDate start, LocalDate end) {
    public TimeRange {
        Objects.requireNonNull(start, "TimeRange start must not be null");
        Objects.requireNonNull(end, "TimeRange end must not be null");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("TimeRange start must not be after end");
        }
    }

    public static TimeRange of(LocalDate start, LocalDate end) {
        return new TimeRange(start, end);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
