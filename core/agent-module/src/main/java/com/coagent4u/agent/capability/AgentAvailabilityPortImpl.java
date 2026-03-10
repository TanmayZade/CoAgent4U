package com.coagent4u.agent.capability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * working hours (09:00–17:00 per day in Asia/Kolkata).
 * </p>
 *
 * <p>
 * <b>Graceful degradation:</b> If the Google Calendar API returns an error
 * (expired tokens, insufficient scopes, rate limits), the agent is treated
 * as <b>fully free</b> during working hours. The coordination flow continues
 * rather than failing.
 * </p>
 */
public class AgentAvailabilityPortImpl implements AgentAvailabilityPort {

    private static final Logger log = LoggerFactory.getLogger(AgentAvailabilityPortImpl.class);

    private static final LocalTime WORK_START = LocalTime.of(9, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final CalendarPort calendarPort;

    public AgentAvailabilityPortImpl(CalendarPort calendarPort) {
        this.calendarPort = calendarPort;
    }

    @Override
    public List<AvailabilityBlock> getAvailability(AgentId agentId, TimeRange range) {
        List<TimeSlot> busySlots;
        try {
            // 1. Get busy slots from calendar
            busySlots = calendarPort.getFreeBusy(agentId, range);
            log.info("[AvailabilityPort] Retrieved {} busy slots for agent {}", busySlots.size(), agentId);
        } catch (Exception e) {
            // Graceful degradation: treat as fully free if calendar unavailable
            log.warn("[AvailabilityPort] Calendar unavailable for agent {} ({}). Treating as fully free.",
                    agentId, e.getMessage());
            return buildFullyFreeBlocks(range);
        }

        // 2. Sort busy slots by start time
        List<TimeSlot> sorted = new ArrayList<>(busySlots);
        sorted.sort(Comparator.comparing(TimeSlot::start));

        // 3. Compute free blocks by inverting busy slots within working hours
        List<AvailabilityBlock> freeBlocks = new ArrayList<>();
        LocalDate current = range.start();
        LocalDate end = range.end();

        while (!current.isAfter(end)) {
            Instant dayStart = ZonedDateTime.of(current, WORK_START, IST).toInstant();
            Instant dayEnd = ZonedDateTime.of(current, WORK_END, IST).toInstant();

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

        log.info("[AvailabilityPort] Computed {} free blocks for agent {} over {} to {}",
                freeBlocks.size(), agentId, range.start(), range.end());
        return freeBlocks;
    }

    /**
     * Returns one fully-free block per day in the range (entire working hours).
     * Used as fallback when calendar access fails.
     */
    private List<AvailabilityBlock> buildFullyFreeBlocks(TimeRange range) {
        List<AvailabilityBlock> freeBlocks = new ArrayList<>();
        LocalDate current = range.start();
        LocalDate end = range.end();

        while (!current.isAfter(end)) {
            Instant dayStart = ZonedDateTime.of(current, WORK_START, IST).toInstant();
            Instant dayEnd = ZonedDateTime.of(current, WORK_END, IST).toInstant();
            freeBlocks.add(new AvailabilityBlock(dayStart, dayEnd));
            current = current.plusDays(1);
        }

        log.info("[AvailabilityPort] Generated {} fully-free fallback blocks", freeBlocks.size());
        return freeBlocks;
    }
}
