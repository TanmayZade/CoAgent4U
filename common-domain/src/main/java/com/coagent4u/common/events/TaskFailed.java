package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired if the agent encounters an unrecoverable error during execution.
 */
public record TaskFailed(
        AgentId agentId,
        UserId userId,
        String taskType,
        String errorMessage,
        Instant occurredAt) implements UserAwareEvent {
    public TaskFailed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static TaskFailed of(AgentId agentId, UserId userId, String taskType, String errorMessage) {
        return new TaskFailed(agentId, userId, taskType, errorMessage, Instant.now());
    }
}
