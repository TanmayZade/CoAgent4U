package com.coagent4u.persistence.coordination;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "coordinations")
public class CoordinationJpaEntity {

    @Id
    @Column(name = "coordination_id")
    private UUID coordinationId;

    @Column(name = "requester_agent_id", nullable = false)
    private UUID requesterAgentId;

    @Column(name = "invitee_agent_id", nullable = false)
    private UUID inviteeAgentId;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    @Column(name = "proposal", columnDefinition = "JSONB")
    private String proposalJson;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "coordination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("transitionedAt ASC")
    private List<StateLogJpaEntity> stateLog = new ArrayList<>();

    protected CoordinationJpaEntity() {
    }

    public CoordinationJpaEntity(UUID coordinationId, UUID requesterAgentId, UUID inviteeAgentId,
            String state, String proposalJson, String reason,
            Instant createdAt, Instant completedAt) {
        this.coordinationId = coordinationId;
        this.requesterAgentId = requesterAgentId;
        this.inviteeAgentId = inviteeAgentId;
        this.state = state;
        this.proposalJson = proposalJson;
        this.reason = reason;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    // Getters & setters
    public UUID getCoordinationId() {
        return coordinationId;
    }

    public void setCoordinationId(UUID id) {
        this.coordinationId = id;
    }

    public UUID getRequesterAgentId() {
        return requesterAgentId;
    }

    public void setRequesterAgentId(UUID id) {
        this.requesterAgentId = id;
    }

    public UUID getInviteeAgentId() {
        return inviteeAgentId;
    }

    public void setInviteeAgentId(UUID id) {
        this.inviteeAgentId = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProposalJson() {
        return proposalJson;
    }

    public void setProposalJson(String json) {
        this.proposalJson = json;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant t) {
        this.createdAt = t;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant t) {
        this.completedAt = t;
    }

    public List<StateLogJpaEntity> getStateLog() {
        return stateLog;
    }

    public void setStateLog(List<StateLogJpaEntity> log) {
        this.stateLog = log;
    }
}
