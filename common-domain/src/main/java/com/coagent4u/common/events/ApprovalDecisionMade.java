package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

/** Published when a user approves or rejects an approval request. */
public record ApprovalDecisionMade(
        ApprovalId approvalId,
        UserId userId,
        String decision, // "APPROVED" | "REJECTED"
        String approvalType, // "PERSONAL" | "COLLABORATIVE"
        Instant occurredAt) implements DomainEvent {
    public ApprovalDecisionMade {
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(approvalType, "approvalType must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ApprovalDecisionMade of(ApprovalId approvalId, UserId userId,
            String decision, String approvalType) {
        return new ApprovalDecisionMade(approvalId, userId, decision, approvalType, Instant.now());
    }
}
