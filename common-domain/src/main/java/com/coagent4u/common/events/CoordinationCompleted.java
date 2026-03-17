package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.EventId;

/**
 * Published when both calendar events are created and coordination reaches
 * COMPLETED.
 */
public record CoordinationCompleted(
        CoordinationId coordinationId,
        EventId eventIdA,
        EventId eventIdB,
        Instant occurredAt) implements CoordinationAwareEvent {
    public CoordinationCompleted {
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(eventIdA, "eventIdA must not be null");
        Objects.requireNonNull(eventIdB, "eventIdB must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationCompleted of(CoordinationId id, EventId eventIdA, EventId eventIdB) {
        return new CoordinationCompleted(id, eventIdA, eventIdB, Instant.now());
    }
}
