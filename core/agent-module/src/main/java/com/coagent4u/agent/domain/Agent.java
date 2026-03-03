package com.coagent4u.agent.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Aggregate root for the Agent bounded context.
 *
 * <p>
 * A personal agent is provisioned 1:1 with a user during registration.
 * It acts as the autonomous executor for calendar management and coordination.
 */
public class Agent {

    public enum Status {
        ACTIVE, INACTIVE
    }

    private final AgentId agentId;
    private final UserId userId;
    private Status status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Agent(AgentId agentId, UserId userId) {
        this.agentId = Objects.requireNonNull(agentId);
        this.userId = Objects.requireNonNull(userId);
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void deactivate() {
        if (status == Status.INACTIVE) {
            throw new IllegalStateException("Agent " + agentId + " is already inactive");
        }
        this.status = Status.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = Status.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    // Getters
    public AgentId getAgentId() {
        return agentId;
    }

    public UserId getUserId() {
        return userId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
