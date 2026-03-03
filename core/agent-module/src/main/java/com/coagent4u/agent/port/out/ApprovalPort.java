package com.coagent4u.agent.port.out;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.Duration;

/**
 * Outbound port — requests and queries approvals for an agent's user.
 * Delegates to the approval-module via the persistence adapter at runtime.
 */
public interface ApprovalPort {
    /**
     * Creates a personal approval request (user approving their own action).
     *
     * @param agentId the agent on whose behalf approval is requested
     * @param context human-readable description shown to the user
     * @param timeout expiry duration
     * @return the created ApprovalId
     */
    ApprovalId requestPersonalApproval(AgentId agentId, String context, Duration timeout);
}
