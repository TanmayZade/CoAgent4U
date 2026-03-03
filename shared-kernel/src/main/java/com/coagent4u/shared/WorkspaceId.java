package com.coagent4u.shared;

import java.util.Objects;

/**
 * Strongly-typed wrapper for a Slack workspace ID.
 */
public record WorkspaceId(String value) {

    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkspaceId value must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
