package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.CoordinationId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on every coordination state machine transition.
 * Consumed by audit handler to persist to coordination_state_log.
 */
public record CoordinationStateChanged(
        UUID eventId,
        Instant occurredAt,
        CoordinationId coordinationId,
        String fromState,
        String toState,
        String triggeredBy
) implements DomainEvent {

    public CoordinationStateChanged(CoordinationId coordinationId, String fromState, String toState, String triggeredBy) {
        this(UUID.randomUUID(), Instant.now(), coordinationId, fromState, toState, triggeredBy);
    }
}
