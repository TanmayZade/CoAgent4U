package com.coagent4u.agent.capability;

import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;

/**
 * Implements {@link AgentEventExecutionPort} (coordination-module's outbound
 * interface)
 * by delegating to the agent's own {@link CalendarPort}.
 */
public class AgentEventExecutionPortImpl implements AgentEventExecutionPort {

    private final CalendarPort calendarPort;

    public AgentEventExecutionPortImpl(CalendarPort calendarPort) {
        this.calendarPort = calendarPort;
    }

    @Override
    public EventId createEvent(AgentId agentId, TimeSlot timeSlot, String title) {
        return calendarPort.createEvent(agentId, timeSlot, title);
    }

    @Override
    public void deleteEvent(AgentId agentId, EventId eventId) {
        calendarPort.deleteEvent(agentId, eventId);
    }
}
