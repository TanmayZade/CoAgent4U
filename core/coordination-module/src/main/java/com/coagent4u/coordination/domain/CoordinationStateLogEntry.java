package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.coagent4u.shared.CoordinationId;

/**
 * Immutable log entry for a single state transition in a Coordination's
 * lifecycle.
 */
public record CoordinationStateLogEntry(
        UUID logId,
        CoordinationId coordinationId,
        CoordinationState fromState, // nullable on INITIATED
        CoordinationState toState,
        String reason,
        Instant timestamp) {
    public CoordinationStateLogEntry {
        Objects.requireNonNull(logId, "logId must not be null");
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(toState, "toState must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static CoordinationStateLogEntry of(CoordinationId id, CoordinationState from,
            CoordinationState to, String reason) {
        return new CoordinationStateLogEntry(UUID.randomUUID(), id, from, to, reason, Instant.now());
    }
}
