package com.coagent4u.persistence.activity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_activities")
public class AgentActivityJpaEntity {

    @Id
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "coordination_id")
    private UUID coordinationId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "level", nullable = false, length = 16)
    private String level;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AgentActivityJpaEntity() {
    }

    public AgentActivityJpaEntity(UUID logId, UUID agentId, UUID correlationId, UUID coordinationId,
            String eventType, String description, String level, Instant occurredAt) {
        this.logId = logId;
        this.agentId = agentId;
        this.correlationId = correlationId;
        this.coordinationId = coordinationId;
        this.eventType = eventType;
        this.description = description;
        this.level = level;
        this.occurredAt = occurredAt;
    }

    public UUID getLogId() {
        return logId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public UUID getCoordinationId() {
        return coordinationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public String getLevel() {
        return level;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
