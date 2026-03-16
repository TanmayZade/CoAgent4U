package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * Fired on successful rule-based parsing.
 */
public record IntentParsed(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        String intentType,
        String rawText,
        Instant occurredAt) implements CorrelationAwareEvent, UserAwareEvent {
    public IntentParsed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(intentType, "intentType must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static IntentParsed of(AgentId agentId, UserId userId, CorrelationId correlationId, String intentType, String rawText) {
        return new IntentParsed(agentId, userId, correlationId, intentType, rawText, Instant.now());
    }
}
