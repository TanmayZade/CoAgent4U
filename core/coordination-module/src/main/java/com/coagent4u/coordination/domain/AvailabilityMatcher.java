package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.coagent4u.shared.Duration;
import com.coagent4u.shared.TimeSlot;

/**
 * Domain service that finds overlapping free time between two agents'
 * availability blocks.
 * Given two lists of free blocks and a required meeting duration, returns the
 * first slot
 * where both agents are free for the full duration.
 */
public class AvailabilityMatcher {

    /**
     * Finds the first overlapping time slot where both agents are simultaneously
     * free
     * for at least {@code requiredDuration}.
     *
     * @param blocksA          free blocks of agent A (sorted ascending by start)
     * @param blocksB          free blocks of agent B (sorted ascending by start)
     * @param requiredDuration minimum duration needed for the meeting
     * @return the first matching TimeSlot, or empty if no match exists
     */
    public Optional<TimeSlot> findOverlap(List<AvailabilityBlock> blocksA,
            List<AvailabilityBlock> blocksB,
            Duration requiredDuration) {
        long requiredMillis = (long) requiredDuration.minutes() * 60_000;

        for (AvailabilityBlock a : blocksA) {
            for (AvailabilityBlock b : blocksB) {
                Instant overlapStart = a.start().isAfter(b.start()) ? a.start() : b.start();
                Instant overlapEnd = a.end().isBefore(b.end()) ? a.end() : b.end();

                if (overlapStart.isBefore(overlapEnd)) {
                    long overlapMs = overlapEnd.toEpochMilli() - overlapStart.toEpochMilli();
                    if (overlapMs >= requiredMillis) {
                        Instant slotEnd = overlapStart.plusMillis(requiredMillis);
                        return Optional.of(new TimeSlot(overlapStart, slotEnd));
                    }
                }
            }
        }
        return Optional.empty();
    }
}
