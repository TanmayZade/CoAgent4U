package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.UserId;

/** Published when a user account is deleted (soft-deleted). */
public record UserDeleted(
        UserId userId,
        Instant occurredAt) implements DomainEvent {
    public UserDeleted {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static UserDeleted of(UserId userId) {
        return new UserDeleted(userId, Instant.now());
    }
}
