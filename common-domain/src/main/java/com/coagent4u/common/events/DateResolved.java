package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * Fired when natural language dates are resolved to an Instant.
 */
public record DateResolved(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        String originalText,
        Instant resolvedInstant,
        Instant occurredAt) implements CorrelationAwareEvent, UserAwareEvent {
    public DateResolved {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(originalText, "originalText must not be null");
        Objects.requireNonNull(resolvedInstant, "resolvedInstant must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static DateResolved of(AgentId agentId, UserId userId, CorrelationId correlationId, String originalText,
            Instant resolvedInstant) {
        return new DateResolved(agentId, userId, correlationId, originalText, resolvedInstant, Instant.now());
    }
}
