package com.coagent4u.coordination.port.out;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;

/**
 * Outbound port — creates and deletes calendar events on behalf of agents.
 * Implemented by agent-module's {@code AgentEventExecutionPortImpl}.
 */
public interface AgentEventExecutionPort {
    EventId createEvent(AgentId agentId, TimeSlot timeSlot, String title);

    void deleteEvent(AgentId agentId, EventId eventId);
}
