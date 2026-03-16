package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique trace identifier for a single agent workflow
 * (e.g., from initial message intake through to event creation or failure).
 */
public record CorrelationId(UUID value) {

    public CorrelationId {
        Objects.requireNonNull(value, "correlationId value must not be null");
    }

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID());
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(UUID.fromString(value));
    }
}
