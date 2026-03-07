package com.coagent4u.agent.port.out;

import java.util.List;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CalendarEvent;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Outbound port — interacts with an external calendar API (Google Calendar).
 * Implemented in calendar-module (GoogleCalendarAdapter).
 */
public interface CalendarPort {
    List<TimeSlot> getEvents(AgentId agentId, TimeRange range);

    List<CalendarEvent> getCalendarEvents(AgentId agentId, TimeRange range);

    List<TimeSlot> getFreeBusy(AgentId agentId, TimeRange range);

    EventId createEvent(AgentId agentId, TimeSlot timeSlot, String title);

    void deleteEvent(AgentId agentId, EventId eventId);
}
