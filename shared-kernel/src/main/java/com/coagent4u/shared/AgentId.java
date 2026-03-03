package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for a personal AI agent.
 */
public record AgentId(UUID value) {

    public AgentId {
        Objects.requireNonNull(value, "AgentId value must not be null");
    }

    public static AgentId generate() {
        return new AgentId(UUID.randomUUID());
    }

    public static AgentId from(String raw) {
        return new AgentId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
