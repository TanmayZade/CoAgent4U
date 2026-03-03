package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user approves or rejects an approval request.
 * Consumed by agent-module to route the decision to the appropriate handler.
 */
public record ApprovalDecisionMade(
        UUID eventId,
        Instant occurredAt,
        ApprovalId approvalId,
        UserId userId,
        String decision,
        String approvalType
) implements DomainEvent {

    public ApprovalDecisionMade(ApprovalId approvalId, UserId userId, String decision, String approvalType) {
        this(UUID.randomUUID(), Instant.now(), approvalId, userId, decision, approvalType);
    }
}
