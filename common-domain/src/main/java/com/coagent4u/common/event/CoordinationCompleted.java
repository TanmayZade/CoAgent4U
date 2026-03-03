package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.CoordinationId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a coordination reaches terminal COMPLETED state.
 * Consumed by notification handler to send confirmation to both users.
 */
public record CoordinationCompleted(
        UUID eventId,
        Instant occurredAt,
        CoordinationId coordinationId
) implements DomainEvent {

    public CoordinationCompleted(CoordinationId coordinationId) {
        this(UUID.randomUUID(), Instant.now(), coordinationId);
    }
}
