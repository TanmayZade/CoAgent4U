package com.coagent4u.agent.port.in;

import java.util.List;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Inbound port — retrieves the user's upcoming calendar events.
 */
public interface ViewScheduleUseCase {
    List<TimeSlot> viewSchedule(AgentId agentId, TimeRange range);
}
