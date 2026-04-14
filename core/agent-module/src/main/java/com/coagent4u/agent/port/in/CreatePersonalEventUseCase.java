package com.coagent4u.agent.port.in;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;

/**
 * Inbound port — creates a personal calendar event for a user after approval.
 */
public interface CreatePersonalEventUseCase {
    EventId createPersonalEvent(AgentId agentId, String title, TimeSlot timeSlot, boolean force);

    void resolveConflict(com.coagent4u.shared.ApprovalId approvalId, com.coagent4u.shared.UserId userId, String resolution);
}
