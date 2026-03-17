package com.coagent4u.coordination.domain;

import java.util.List;

/**
 * Result of an availability check, containing both the free time blocks
 * and the count of busy events (constraints) found on the calendar.
 */
public record AvailabilityResult(List<AvailabilityBlock> freeBlocks, int busyCount) {
}
