package com.coagent4u.approval.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

/**
 * Aggregate root for the Approval bounded context.
 *
 * <p>
 * An approval represents a user's explicit consent for an action:
 * either a personal calendar change (PERSONAL) or a meeting proposal
 * (COLLABORATIVE).
 * Once a decision is made (APPROVED / REJECTED / EXPIRED), the state is
 * terminal.
 */
public class Approval {

    private final ApprovalId approvalId;
    private final CoordinationId coordinationId; // null for PERSONAL approvals
    private final UserId userId;
    private final ApprovalType approvalType;
    private ApprovalStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant decidedAt;

    public Approval(ApprovalId approvalId, CoordinationId coordinationId,
            UserId userId, ApprovalType approvalType, Instant expiresAt) {
        this.approvalId = Objects.requireNonNull(approvalId);
        this.coordinationId = coordinationId; // nullable for PERSONAL
        this.userId = Objects.requireNonNull(userId);
        this.approvalType = Objects.requireNonNull(approvalType);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.status = ApprovalStatus.PENDING;
        this.createdAt = Instant.now();
    }

    /**
     * Records a user's APPROVED or REJECTED decision.
     *
     * @throws IllegalStateException    if status is not PENDING
     * @throws IllegalArgumentException if decision is EXPIRED or PENDING
     */
    public void decide(ApprovalStatus decision) {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval " + approvalId + " is already " + status);
        }
        if (decision != ApprovalStatus.APPROVED && decision != ApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED, got: " + decision);
        }
        this.status = decision;
        this.decidedAt = Instant.now();
    }

    /**
     * Transitions this approval to EXPIRED state.
     *
     * @throws IllegalStateException if status is not PENDING
     */
    public void expire() {
        if (this.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot expire approval " + approvalId + " in state " + status);
        }
        this.status = ApprovalStatus.EXPIRED;
        this.decidedAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return ExpirationPolicy.isExpired(this, now);
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    // Getters
    public ApprovalId getApprovalId() {
        return approvalId;
    }

    public CoordinationId getCoordinationId() {
        return coordinationId;
    }

    public UserId getUserId() {
        return userId;
    }

    public ApprovalType getApprovalType() {
        return approvalType;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
