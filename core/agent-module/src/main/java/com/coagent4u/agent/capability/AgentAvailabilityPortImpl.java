package com.coagent4u.agent.capability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Implements {@link AgentAvailabilityPort} by delegating to the agent's own
 * {@link CalendarPort}.
 *
 * <p>
 * Agent Sovereignty: only agent-module calls CalendarPort; coordination-module
 * interacts exclusively through this port.
 * </p>
 *
 * <p>
 * Converts busy/occupied time slots from the calendar into <b>free</b>
 * {@link AvailabilityBlock}s by computing the gaps between busy slots within
 * working hours (09:00–18:00 per day).
 * </p>
 */
public class AgentAvailabilityPortImpl implements AgentAvailabilityPort {

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(18, 0);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("UTC");

    private final CalendarPort calendarPort;

    public AgentAvailabilityPortImpl(CalendarPort calendarPort) {
        this.calendarPort = calendarPort;
    }

    @Override
    public List<AvailabilityBlock> getAvailability(AgentId agentId, TimeRange range) {
        // 1. Get busy slots from calendar
        List<TimeSlot> busySlots = calendarPort.getFreeBusy(agentId, range);

        // 2. Sort busy slots by start time
        List<TimeSlot> sorted = new ArrayList<>(busySlots);
        sorted.sort(Comparator.comparing(TimeSlot::start));

        // 3. Compute free blocks by inverting busy slots within working hours
        List<AvailabilityBlock> freeBlocks = new ArrayList<>();
        LocalDate current = range.start();
        LocalDate end = range.end();

        while (!current.isAfter(end)) {
            Instant dayStart = ZonedDateTime.of(current, WORK_START, DEFAULT_ZONE).toInstant();
            Instant dayEnd = ZonedDateTime.of(current, WORK_END, DEFAULT_ZONE).toInstant();

            // Find busy slots that overlap with this day's working hours
            List<TimeSlot> dayBusy = new ArrayList<>();
            for (TimeSlot busy : sorted) {
                if (busy.end().isAfter(dayStart) && busy.start().isBefore(dayEnd)) {
                    dayBusy.add(busy);
                }
            }

            // Compute free gaps
            Instant cursor = dayStart;
            for (TimeSlot busy : dayBusy) {
                Instant busyStart = busy.start().isBefore(dayStart) ? dayStart : busy.start();
                Instant busyEnd = busy.end().isAfter(dayEnd) ? dayEnd : busy.end();

                if (cursor.isBefore(busyStart)) {
                    freeBlocks.add(new AvailabilityBlock(cursor, busyStart));
                }
                if (busyEnd.isAfter(cursor)) {
                    cursor = busyEnd;
                }
            }

            // Remaining time after last busy slot
            if (cursor.isBefore(dayEnd)) {
                freeBlocks.add(new AvailabilityBlock(cursor, dayEnd));
            }

            current = current.plusDays(1);
        }

        return freeBlocks;
    }
}
