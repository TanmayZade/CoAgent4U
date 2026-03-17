package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.shared.CoordinationId;

/**
 * Published when a coordination reaches a terminal FAILED or REJECTED state.
 */
public record CoordinationFailed(
        CoordinationId coordinationId,
        String reason,
        Instant occurredAt) implements CoordinationAwareEvent {
    public CoordinationFailed {
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationFailed of(CoordinationId id, String reason) {
        return new CoordinationFailed(id, reason, Instant.now());
    }
}
