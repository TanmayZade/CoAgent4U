package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired if the Calendar API rejects or fails personal event creation.
 */
public record PersonalEventFailed(
        AgentId agentId,
        UserId userId,
        String title,
        String errorMessage,
        Instant occurredAt) implements UserAwareEvent {
    public PersonalEventFailed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static PersonalEventFailed of(AgentId agentId, UserId userId, String title, String errorMessage) {
        return new PersonalEventFailed(agentId, userId, title, errorMessage, Instant.now());
    }
}
