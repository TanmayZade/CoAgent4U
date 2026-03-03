package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when an approval request expires (12-hour timeout reached).
 * Consumed by agent-module to terminate the associated coordination.
 */
public record ApprovalExpired(
        UUID eventId,
        Instant occurredAt,
        ApprovalId approvalId,
        UserId userId,
        String approvalType
) implements DomainEvent {

    public ApprovalExpired(ApprovalId approvalId, UserId userId, String approvalType) {
        this(UUID.randomUUID(), Instant.now(), approvalId, userId, approvalType);
    }
}
