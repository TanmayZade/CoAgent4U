package com.coagent4u.agent.domain;

import java.util.List;

import com.coagent4u.shared.TimeSlot;

/**
 * Domain service that detects whether a proposed time slot conflicts with
 * any existing calendar events.
 */
public class ConflictDetector {

    /**
     * @param existingSlots list of already-booked time slots
     * @param proposed      the time slot the user wants to create
     * @return true if {@code proposed} overlaps with any existing slot
     */
    public boolean hasConflict(List<TimeSlot> existingSlots, TimeSlot proposed) {
        return existingSlots.stream().anyMatch(existing -> existing.overlaps(proposed));
    }
}
