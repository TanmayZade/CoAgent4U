package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.CorrelationAwareEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CorrelationId;
import com.coagent4u.shared.UserId;

/**
 * Fired when the agent sends an interactive approval request to its user.
 */
public record PersonalApprovalRequested(
        AgentId agentId,
        UserId userId,
        CorrelationId correlationId,
        ApprovalId approvalId,
        String proposalSummary,
        Instant occurredAt) implements CorrelationAwareEvent, UserAwareEvent {
    public PersonalApprovalRequested {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(proposalSummary, "proposalSummary must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static PersonalApprovalRequested of(AgentId agentId, UserId userId, CorrelationId correlationId, ApprovalId approvalId,
            String proposalSummary) {
        return new PersonalApprovalRequested(agentId, userId, correlationId, approvalId, proposalSummary, Instant.now());
    }
}
