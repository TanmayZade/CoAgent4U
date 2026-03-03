package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.CoordinationId;

/** Published on every state machine transition of a Coordination. */
public record CoordinationStateChanged(
        CoordinationId coordinationId,
        String fromState,
        String toState,
        String reason,
        Instant occurredAt) implements DomainEvent {
    public CoordinationStateChanged {
        Objects.requireNonNull(coordinationId, "coordinationId must not be null");
        Objects.requireNonNull(toState, "toState must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static CoordinationStateChanged of(CoordinationId id, String fromState,
            String toState, String reason) {
        return new CoordinationStateChanged(id, fromState, toState, reason, Instant.now());
    }
}
