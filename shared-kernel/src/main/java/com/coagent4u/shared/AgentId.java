package com.coagent4u.shared;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for an Agent. */
public record AgentId(UUID value) {
    public AgentId {
        Objects.requireNonNull(value, "AgentId value must not be null");
    }

    public static AgentId generate() {
        return new AgentId(UUID.randomUUID());
    }

    public static AgentId of(UUID value) {
        return new AgentId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
