package com.coagent4u.shared;

import java.util.Objects;

/** Slack workspace / team identifier (e.g. "T01ABC123"). */
public record WorkspaceId(String value) {
    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value must not be null");
        if (value.isBlank())
            throw new IllegalArgumentException("WorkspaceId value must not be blank");
    }

    public static WorkspaceId of(String value) {
        return new WorkspaceId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
