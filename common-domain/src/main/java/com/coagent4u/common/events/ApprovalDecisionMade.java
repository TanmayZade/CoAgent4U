package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

/** Published when a user approves or rejects an approval request. */
public record ApprovalDecisionMade(
        ApprovalId approvalId,
        UserId userId,
        String decision, // "APPROVED" | "REJECTED"
        String approvalType, // "PERSONAL" | "COLLABORATIVE"
        CoordinationId coordinationId, // non-null for COLLABORATIVE
        Instant occurredAt) implements CoordinationAwareEvent {
    public ApprovalDecisionMade {
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(approvalType, "approvalType must not be null");
        // coordinationId is nullable for PERSONAL approvals
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ApprovalDecisionMade of(ApprovalId approvalId, UserId userId,
            String decision, String approvalType) {
        return new ApprovalDecisionMade(approvalId, userId, decision, approvalType, null, Instant.now());
    }

    public static ApprovalDecisionMade of(ApprovalId approvalId, UserId userId,
            String decision, String approvalType, CoordinationId coordinationId) {
        return new ApprovalDecisionMade(approvalId, userId, decision, approvalType, coordinationId, Instant.now());
    }
}
