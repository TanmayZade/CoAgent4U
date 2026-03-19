package com.coagent4u.coordination.port.out;

import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;

/**
 * Outbound port — requests user approval for a meeting proposal via an agent.
 * Implemented by agent-module's {@code AgentApprovalPortImpl}.
 */
public interface AgentApprovalPort {
    ApprovalRequestResult requestApproval(AgentId agentId, MeetingProposal proposal);
}
