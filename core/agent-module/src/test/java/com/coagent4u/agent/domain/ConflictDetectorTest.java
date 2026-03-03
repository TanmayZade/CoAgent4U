package com.coagent4u.agent.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coagent4u.shared.TimeSlot;

class ConflictDetectorTest {

    private final ConflictDetector detector = new ConflictDetector();

    @Test
    void hasConflict_overlapping() {
        TimeSlot existing = new TimeSlot(
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T11:00:00Z"));
        TimeSlot proposed = new TimeSlot(
                Instant.parse("2026-03-01T10:30:00Z"),
                Instant.parse("2026-03-01T11:30:00Z"));
        assertTrue(detector.hasConflict(List.of(existing), proposed));
    }

    @Test
    void noConflict_nonOverlapping() {
        TimeSlot existing = new TimeSlot(
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T11:00:00Z"));
        TimeSlot proposed = new TimeSlot(
                Instant.parse("2026-03-01T11:00:00Z"),
                Instant.parse("2026-03-01T12:00:00Z"));
        assertFalse(detector.hasConflict(List.of(existing), proposed));
    }

    @Test
    void noConflict_emptyList() {
        TimeSlot proposed = new TimeSlot(
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T11:00:00Z"));
        assertFalse(detector.hasConflict(List.of(), proposed));
    }

    @Test
    void hasConflict_multipleExisting() {
        TimeSlot e1 = new TimeSlot(Instant.parse("2026-03-01T09:00:00Z"), Instant.parse("2026-03-01T10:00:00Z"));
        TimeSlot e2 = new TimeSlot(Instant.parse("2026-03-01T11:00:00Z"), Instant.parse("2026-03-01T12:00:00Z"));
        TimeSlot proposed = new TimeSlot(Instant.parse("2026-03-01T11:30:00Z"), Instant.parse("2026-03-01T12:30:00Z"));
        assertTrue(detector.hasConflict(List.of(e1, e2), proposed));
    }
}
