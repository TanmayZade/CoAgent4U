package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.TimeSlot;

/** Value object representing a free/busy time block for an agent's calendar. */
public record AvailabilityBlock(Instant start, Instant end) {
    public AvailabilityBlock {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (!start.isBefore(end))
            throw new IllegalArgumentException("start must be before end");
    }

    public TimeSlot toTimeSlot() {
        return new TimeSlot(start, end);
    }

    public boolean overlaps(AvailabilityBlock other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public java.time.Duration duration() {
        return java.time.Duration.between(start, end);
    }
}
