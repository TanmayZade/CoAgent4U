package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user deletes their account (GDPR Right to Erasure).
 * Consumed by agent-module to delete the associated agent.
 */
public record UserDeleted(
        UUID eventId,
        Instant occurredAt,
        UserId userId
) implements DomainEvent {

    public UserDeleted(UserId userId) {
        this(UUID.randomUUID(), Instant.now(), userId);
    }
}
