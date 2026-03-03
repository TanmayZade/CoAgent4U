package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for a coordination instance (A2A negotiation).
 */
public record CoordinationId(UUID value) {

    public CoordinationId {
        Objects.requireNonNull(value, "CoordinationId value must not be null");
    }

    public static CoordinationId generate() {
        return new CoordinationId(UUID.randomUUID());
    }

    public static CoordinationId from(String raw) {
        return new CoordinationId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
