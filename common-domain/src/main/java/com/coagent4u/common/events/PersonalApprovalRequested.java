package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

/**
 * Fired when an interactive Slack approval card is sent to the user
 * for personal event creation or for double-confirmation during coordination.
 */
public record PersonalApprovalRequested(
        AgentId agentId,
        UserId userId,
        ApprovalId approvalId,
        String proposalSummary,
        Instant occurredAt) implements UserAwareEvent {
    public PersonalApprovalRequested {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        Objects.requireNonNull(proposalSummary, "proposalSummary must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static PersonalApprovalRequested of(AgentId agentId, UserId userId, ApprovalId approvalId,
            String proposalSummary) {
        return new PersonalApprovalRequested(agentId, userId, approvalId, proposalSummary, Instant.now());
    }
}
