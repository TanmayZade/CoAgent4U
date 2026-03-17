package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CoordinationAwareEvent;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.UserId;

/**
 * Published when an approval request expires before a user decision is
 * recorded.
 */
public record ApprovalExpired(
        ApprovalId approvalId,
        UserId userId,
        String approvalType, // "PERSONAL" | "COLLABORATIVE"
        CoordinationId coordinationId, // non-null for COLLABORATIVE
        Instant expiredAt,
        Instant occurredAt) implements CoordinationAwareEvent {
    public ApprovalExpired {
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(approvalType, "approvalType must not be null");
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static ApprovalExpired of(ApprovalId approvalId, UserId userId,
            String approvalType, Instant expiredAt) {
        return new ApprovalExpired(approvalId, userId, approvalType, null, expiredAt, Instant.now());
    }

    public static ApprovalExpired of(ApprovalId approvalId, UserId userId,
            String approvalType, CoordinationId coordinationId, Instant expiredAt) {
        return new ApprovalExpired(approvalId, userId, approvalType, coordinationId, expiredAt, Instant.now());
    }
}
