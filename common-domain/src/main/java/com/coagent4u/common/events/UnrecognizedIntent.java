package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired if both rule-based and LLM parsing tiers fail to recognize intent.
 */
public record UnrecognizedIntent(
        AgentId agentId,
        UserId userId,
        String rawText,
        Instant occurredAt) implements UserAwareEvent {
    public UnrecognizedIntent {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static UnrecognizedIntent of(AgentId agentId, UserId userId, String rawText) {
        return new UnrecognizedIntent(agentId, userId, rawText, Instant.now());
    }
}
