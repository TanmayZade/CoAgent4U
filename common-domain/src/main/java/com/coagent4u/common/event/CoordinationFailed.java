package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.CoordinationId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a coordination fails (rejection, timeout, or error).
 * Consumed by notification handler to send failure notification.
 */
public record CoordinationFailed(
        UUID eventId,
        Instant occurredAt,
        CoordinationId coordinationId,
        String reason
) implements DomainEvent {

    public CoordinationFailed(CoordinationId coordinationId, String reason) {
        this(UUID.randomUUID(), Instant.now(), coordinationId, reason);
    }
}
