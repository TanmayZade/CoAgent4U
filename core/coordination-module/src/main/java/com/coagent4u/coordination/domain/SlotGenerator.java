package com.coagent4u.coordination.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.shared.TimeSlot;

/**
 * Domain service that generates fixed-duration time slots within office hours
 * for a given date and timezone.
 *
 * <p>
 * Example: For 09:00–17:00 with 60-minute duration, generates 8 slots:
 * 09:00–10:00, 10:00–11:00, ..., 16:00–17:00
 * </p>
 */
public class SlotGenerator {

    private static final Logger log = LoggerFactory.getLogger(SlotGenerator.class);

    /** Default office hours */
    public static final LocalTime DEFAULT_OFFICE_START = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_OFFICE_END = LocalTime.of(17, 0);
    public static final int DEFAULT_DURATION_MINUTES = 60;

    /**
     * Generates office-hour time slots for a given date.
     *
     * @param date            the target date
     * @param officeStart     start of office hours (e.g. 09:00)
     * @param officeEnd       end of office hours (e.g. 17:00)
     * @param durationMinutes meeting duration in minutes
     * @param timezone        the timezone (e.g. "Asia/Kolkata")
     * @return list of non-overlapping TimeSlot objects covering office hours
     */
    public List<TimeSlot> generateSlots(LocalDate date, LocalTime officeStart,
            LocalTime officeEnd, int durationMinutes,
            ZoneId timezone) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be > 0, got: " + durationMinutes);
        }
        if (!officeStart.isBefore(officeEnd)) {
            throw new IllegalArgumentException("officeStart must be before officeEnd");
        }

        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = officeStart;

        while (current.plusMinutes(durationMinutes).compareTo(officeEnd) <= 0) {
            ZonedDateTime startZdt = ZonedDateTime.of(date, current, timezone);
            ZonedDateTime endZdt = startZdt.plusMinutes(durationMinutes);

            slots.add(new TimeSlot(startZdt.toInstant(), endZdt.toInstant()));
            current = current.plusMinutes(durationMinutes);
        }

        log.info("[SlotGenerator] Generated {} office-hour slots ({}-{}) for {} in {}",
                slots.size(), officeStart, officeEnd, date, timezone);
        return Collections.unmodifiableList(slots);
    }

    /**
     * Generates slots using default office hours (09:00–17:00, 60 min).
     */
    public List<TimeSlot> generateDefaultSlots(LocalDate date, ZoneId timezone) {
        return generateSlots(date, DEFAULT_OFFICE_START, DEFAULT_OFFICE_END,
                DEFAULT_DURATION_MINUTES, timezone);
    }
}
