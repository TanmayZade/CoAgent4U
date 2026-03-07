package com.coagent4u.persistence.approval;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approvals")
public class ApprovalJpaEntity {

    @Id
    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "coordination_id")
    private UUID coordinationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "approval_type", nullable = false, length = 16)
    private String approvalType;

    @Column(name = "decision", nullable = false, length = 16)
    private String decision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_details", columnDefinition = "JSONB")
    private String eventDetailsJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApprovalJpaEntity() {
    }

    public ApprovalJpaEntity(UUID approvalId, UUID coordinationId, UUID userId,
            String approvalType, String decision,
            Instant expiresAt, Instant decidedAt, Instant createdAt) {
        this.approvalId = approvalId;
        this.coordinationId = coordinationId;
        this.userId = userId;
        this.approvalType = approvalType;
        this.decision = decision;
        this.expiresAt = expiresAt;
        this.decidedAt = decidedAt;
        this.createdAt = createdAt;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(UUID id) {
        this.approvalId = id;
    }

    public UUID getCoordinationId() {
        return coordinationId;
    }

    public void setCoordinationId(UUID id) {
        this.coordinationId = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID id) {
        this.userId = id;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String t) {
        this.approvalType = t;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String d) {
        this.decision = d;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant t) {
        this.expiresAt = t;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant t) {
        this.decidedAt = t;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant t) {
        this.createdAt = t;
    }
}
