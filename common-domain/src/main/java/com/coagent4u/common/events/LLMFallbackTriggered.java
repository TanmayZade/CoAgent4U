package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * Fired when rule-based parsing fails and LLM is invoked.
 */
public record LLMFallbackTriggered(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        String rawText,
        String llmResponse,
        Instant occurredAt) implements CorrelationAwareEvent, UserAwareEvent {
    public LLMFallbackTriggered {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(llmResponse, "llmResponse must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static LLMFallbackTriggered of(AgentId agentId, UserId userId, CorrelationId correlationId, String rawText, String llmResponse) {
        return new LLMFallbackTriggered(agentId, userId, correlationId, rawText, llmResponse, Instant.now());
    }
}
