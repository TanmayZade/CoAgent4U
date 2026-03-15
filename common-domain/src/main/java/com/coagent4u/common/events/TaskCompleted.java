package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Fired when the agent successfully finishes its entire workflow.
 */
public record TaskCompleted(
        AgentId agentId,
        UserId userId,
        String taskType,
        String summary,
        Instant occurredAt) implements UserAwareEvent {
    public TaskCompleted {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static TaskCompleted of(AgentId agentId, UserId userId, String taskType, String summary) {
        return new TaskCompleted(agentId, userId, taskType, summary, Instant.now());
    }
}
