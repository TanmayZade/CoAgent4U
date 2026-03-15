package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired the moment the agent "wakes up" to begin processing a request.
 */
public record AgentActivated(
        AgentId agentId,
        UserId userId,
        String source,
        String rawText,
        Instant occurredAt) implements UserAwareEvent {
    public AgentActivated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(rawText, "rawText must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static AgentActivated of(AgentId agentId, UserId userId, String source, String rawText) {
        return new AgentActivated(agentId, userId, source, rawText, Instant.now());
    }
}
