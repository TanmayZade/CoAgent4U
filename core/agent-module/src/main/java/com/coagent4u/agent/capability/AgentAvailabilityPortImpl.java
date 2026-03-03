package com.coagent4u.agent.capability;

import java.util.List;
import java.util.stream.Collectors;

import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeRange;

/**
 * Implements {@link AgentAvailabilityPort} (coordination-module's outbound
 * interface)
 * by delegating to the agent's own {@link CalendarPort}.
 *
 * <p>
 * This class lives in agent-module, bridging coordination's outbound ports
 * with agent's infrastructure adapters. No Spring annotations.
 */
public class AgentAvailabilityPortImpl implements AgentAvailabilityPort {

    private final CalendarPort calendarPort;

    public AgentAvailabilityPortImpl(CalendarPort calendarPort) {
        this.calendarPort = calendarPort;
    }

    @Override
    public List<AvailabilityBlock> getAvailability(AgentId agentId, TimeRange range) {
        // Free/Busy slots from Google Calendar — invert to get "free" windows
        var busySlots = calendarPort.getFreeBusy(agentId, range);

        // Convert busy TimeSlots to free AvailabilityBlocks (simplified: return
        // working-hours blocks minus busy)
        // Full implementation in Phase 3 — for now return empty to unblock compilation
        return busySlots.stream()
                .map(slot -> new AvailabilityBlock(slot.start(), slot.end()))
                .collect(Collectors.toList());
    }
}
