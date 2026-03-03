package com.coagent4u.coordination.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.coagent4u.shared.Duration;
import com.coagent4u.shared.TimeSlot;

class AvailabilityMatcherTest {

    private final AvailabilityMatcher matcher = new AvailabilityMatcher();

    @Test
    void overlappingBlocks_findSlot() {
        AvailabilityBlock a = new AvailabilityBlock(
                Instant.parse("2026-03-01T09:00:00Z"),
                Instant.parse("2026-03-01T12:00:00Z"));
        AvailabilityBlock b = new AvailabilityBlock(
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T14:00:00Z"));
        Optional<TimeSlot> result = matcher.findOverlap(List.of(a), List.of(b), Duration.of(60));
        assertTrue(result.isPresent());
        assertEquals(Instant.parse("2026-03-01T10:00:00Z"), result.get().start());
    }

    @Test
    void noOverlap_returnsEmpty() {
        AvailabilityBlock a = new AvailabilityBlock(
                Instant.parse("2026-03-01T09:00:00Z"),
                Instant.parse("2026-03-01T10:00:00Z"));
        AvailabilityBlock b = new AvailabilityBlock(
                Instant.parse("2026-03-01T11:00:00Z"),
                Instant.parse("2026-03-01T12:00:00Z"));
        Optional<TimeSlot> result = matcher.findOverlap(List.of(a), List.of(b), Duration.of(30));
        assertTrue(result.isEmpty());
    }

    @Test
    void overlapTooShort_returnsEmpty() {
        AvailabilityBlock a = new AvailabilityBlock(
                Instant.parse("2026-03-01T09:00:00Z"),
                Instant.parse("2026-03-01T10:00:00Z"));
        AvailabilityBlock b = new AvailabilityBlock(
                Instant.parse("2026-03-01T09:30:00Z"),
                Instant.parse("2026-03-01T10:30:00Z"));
        // Overlap: 9:30→10:00 = 30 min, but need 60 min
        Optional<TimeSlot> result = matcher.findOverlap(List.of(a), List.of(b), Duration.of(60));
        assertTrue(result.isEmpty());
    }

    @Test
    void multipleBlocks_findsFirst() {
        AvailabilityBlock a1 = new AvailabilityBlock(
                Instant.parse("2026-03-01T09:00:00Z"), Instant.parse("2026-03-01T09:30:00Z"));
        AvailabilityBlock a2 = new AvailabilityBlock(
                Instant.parse("2026-03-01T14:00:00Z"), Instant.parse("2026-03-01T16:00:00Z"));
        AvailabilityBlock b1 = new AvailabilityBlock(
                Instant.parse("2026-03-01T14:30:00Z"), Instant.parse("2026-03-01T17:00:00Z"));
        Optional<TimeSlot> result = matcher.findOverlap(List.of(a1, a2), List.of(b1), Duration.of(60));
        assertTrue(result.isPresent());
        assertEquals(Instant.parse("2026-03-01T14:30:00Z"), result.get().start());
    }
}
