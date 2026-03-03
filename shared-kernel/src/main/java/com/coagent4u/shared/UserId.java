package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for a registered user.
 * Wraps a UUID to prevent accidental use of raw UUIDs across domain boundaries.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(String raw) {
        return new UserId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
