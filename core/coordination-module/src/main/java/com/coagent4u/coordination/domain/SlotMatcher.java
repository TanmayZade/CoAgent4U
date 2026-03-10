package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.shared.TimeSlot;

/**
 * Domain service that filters office-hour slots by checking which ones
 * fall within both agents' free (availability) blocks.
 *
 * <p>
 * Note: {@link AgentAvailabilityPortImpl} returns <b>free</b> blocks
 * (gaps between busy slots). A slot is available if it fits entirely
 * within at least one free block of BOTH agents.
 * </p>
 */
public class SlotMatcher {

    private static final Logger log = LoggerFactory.getLogger(SlotMatcher.class);

    /**
     * Filters office-hour slots, keeping only those that fall entirely
     * within a free block for both agents.
     *
     * @param officeSlots all generated office-hour slots
     * @param freeA       free intervals for agent A (requester)
     * @param freeB       free intervals for agent B (invitee)
     * @return list of slots where both agents are free
     */
    public List<TimeSlot> matchSlots(List<TimeSlot> officeSlots,
            List<AvailabilityBlock> freeA,
            List<AvailabilityBlock> freeB) {
        List<TimeSlot> available = new ArrayList<>();

        for (TimeSlot slot : officeSlots) {
            boolean fitsA = fitsInFreeBlock(slot, freeA);
            boolean fitsB = fitsInFreeBlock(slot, freeB);

            if (fitsA && fitsB) {
                available.add(slot);
            }
        }

        log.info("[SlotMatcher] Matched {} free slots out of {} total (freeBlocksA={}, freeBlocksB={})",
                available.size(), officeSlots.size(), freeA.size(), freeB.size());
        return Collections.unmodifiableList(available);
    }

    /**
     * Checks if a slot fits entirely within at least one free block.
     */
    private boolean fitsInFreeBlock(TimeSlot slot, List<AvailabilityBlock> freeBlocks) {
        Instant slotStart = slot.start();
        Instant slotEnd = slot.end();

        for (AvailabilityBlock free : freeBlocks) {
            // Slot must be entirely within the free block
            if (!slotStart.isBefore(free.start()) && !slotEnd.isAfter(free.end())) {
                return true;
            }
        }
        return false;
    }
}
