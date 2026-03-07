package com.coagent4u.persistence.agent;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity for the event_proposals table.
 */
@Entity
@Table(name = "event_proposals")
public class EventProposalJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EventProposalJpaEntity() {
    } // JPA

    public EventProposalJpaEntity(UUID id, UUID agentId, UUID approvalId,
            String title, Instant startTime, Instant endTime,
            String status, String eventId,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.agentId = agentId;
        this.approvalId = approvalId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters for updates
    public void setApprovalId(UUID approvalId) {
        this.approvalId = approvalId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
