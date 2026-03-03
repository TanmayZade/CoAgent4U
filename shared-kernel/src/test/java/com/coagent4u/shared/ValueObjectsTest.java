package com.coagent4u.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Tests for all shared-kernel value objects — constructor validation and
 * factory methods.
 */
class ValueObjectsTest {

    // ── UserId ──
    @Test
    void userId_validCreation() {
        assertNotNull(UserId.generate().value());
    }

    @Test
    void userId_nullRejects() {
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }

    // ── AgentId ──
    @Test
    void agentId_validCreation() {
        assertNotNull(AgentId.generate().value());
    }

    @Test
    void agentId_nullRejects() {
        assertThrows(NullPointerException.class, () -> new AgentId(null));
    }

    // ── CoordinationId ──
    @Test
    void coordinationId_valid() {
        assertNotNull(CoordinationId.generate());
    }

    @Test
    void coordinationId_null() {
        assertThrows(NullPointerException.class, () -> new CoordinationId(null));
    }

    // ── ApprovalId ──
    @Test
    void approvalId_valid() {
        assertNotNull(ApprovalId.generate());
    }

    @Test
    void approvalId_null() {
        assertThrows(NullPointerException.class, () -> new ApprovalId(null));
    }

    // ── EventId ──
    @Test
    void eventId_valid() {
        assertEquals("abc-123", EventId.of("abc-123").value());
    }

    @Test
    void eventId_null() {
        assertThrows(NullPointerException.class, () -> new EventId(null));
    }

    @Test
    void eventId_blank() {
        assertThrows(IllegalArgumentException.class, () -> new EventId("  "));
    }

    // ── Email ──
    @Test
    void email_valid() {
        assertEquals("test@example.com", Email.of("test@example.com").value());
    }

    @Test
    void email_null() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    void email_invalid() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid"));
    }

    @Test
    void email_noAt() {
        assertThrows(IllegalArgumentException.class, () -> new Email("nodomain"));
    }

    // ── SlackUserId ──
    @Test
    void slackUserId_valid() {
        assertEquals("U01ABC", SlackUserId.of("U01ABC").value());
    }

    @Test
    void slackUserId_null() {
        assertThrows(NullPointerException.class, () -> new SlackUserId(null));
    }

    @Test
    void slackUserId_blank() {
        assertThrows(IllegalArgumentException.class, () -> new SlackUserId(""));
    }

    // ── WorkspaceId ──
    @Test
    void workspaceId_valid() {
        assertEquals("T01XYZ", WorkspaceId.of("T01XYZ").value());
    }

    @Test
    void workspaceId_null() {
        assertThrows(NullPointerException.class, () -> new WorkspaceId(null));
    }

    @Test
    void workspaceId_blank() {
        assertThrows(IllegalArgumentException.class, () -> new WorkspaceId("  "));
    }

    // ── TimeSlot ──
    @Test
    void timeSlot_valid() {
        Instant start = Instant.parse("2026-03-01T10:00:00Z");
        Instant end = Instant.parse("2026-03-01T11:00:00Z");
        TimeSlot slot = new TimeSlot(start, end);
        assertEquals(start, slot.start());
        assertEquals(end, slot.end());
    }

    @Test
    void timeSlot_startAfterEnd() {
        Instant t = Instant.now();
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(t.plusSeconds(60), t));
    }

    @Test
    void timeSlot_overlap() {
        TimeSlot a = new TimeSlot(Instant.parse("2026-03-01T10:00:00Z"), Instant.parse("2026-03-01T11:00:00Z"));
        TimeSlot b = new TimeSlot(Instant.parse("2026-03-01T10:30:00Z"), Instant.parse("2026-03-01T11:30:00Z"));
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void timeSlot_noOverlap() {
        TimeSlot a = new TimeSlot(Instant.parse("2026-03-01T10:00:00Z"), Instant.parse("2026-03-01T11:00:00Z"));
        TimeSlot b = new TimeSlot(Instant.parse("2026-03-01T11:00:00Z"), Instant.parse("2026-03-01T12:00:00Z"));
        assertFalse(a.overlaps(b));
    }

    // ── TimeRange ──
    @Test
    void timeRange_valid() {
        TimeRange r = TimeRange.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7));
        assertTrue(r.contains(LocalDate.of(2026, 3, 3)));
        assertFalse(r.contains(LocalDate.of(2026, 3, 8)));
    }

    @Test
    void timeRange_startAfterEnd() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimeRange(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 1)));
    }

    // ── Duration ──
    @Test
    void duration_valid() {
        assertEquals(30, Duration.of(30).minutes());
    }

    @Test
    void duration_zero() {
        assertThrows(IllegalArgumentException.class, () -> new Duration(0));
    }

    @Test
    void duration_negative() {
        assertThrows(IllegalArgumentException.class, () -> new Duration(-5));
    }

    @Test
    void duration_ofHours() {
        assertEquals(120, Duration.ofHours(2).minutes());
    }
}
