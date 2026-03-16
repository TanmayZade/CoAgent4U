package com.coagent4u.common.events;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Tests instantiation and field access for all 9 domain events.
 */
class DomainEventsTest {

    @Test
    void userRegistered_instantiation() {
        var e = UserRegistered.of(UserId.generate(), "alice", Email.of("a@b.com"),
                SlackUserId.of("U1"), WorkspaceId.of("T1"));
        assertInstanceOf(DomainEvent.class, e);
        assertNotNull(e.occurredAt());
        assertEquals("alice", e.username());
    }

    @Test
    void userDeleted_instantiation() {
        var e = UserDeleted.of(UserId.generate());
        assertNotNull(e.occurredAt());
        assertNotNull(e.userId());
    }

    @Test
    void agentProvisioned_instantiation() {
        var e = AgentProvisioned.of(AgentId.generate(), UserId.generate());
        assertNotNull(e.agentId());
        assertNotNull(e.userId());
    }

    @Test
    void personalEventCreated_instantiation() {
        var e = PersonalEventCreated.of(AgentId.generate(), UserId.generate(), com.coagent4u.shared.CorrelationId.generate(),
                EventId.of("gcal-123"),
                new TimeSlot(Instant.now(), Instant.now().plusSeconds(3600)), "Standup");
        assertEquals("Standup", e.title());
        assertEquals("gcal-123", e.calendarEventId().value());
    }

    @Test
    void coordinationStateChanged_instantiation() {
        var e = CoordinationStateChanged.of(CoordinationId.generate(), "INITIATED",
                "CHECKING_AVAILABILITY_A", "Starting");
        assertEquals("INITIATED", e.fromState());
        assertEquals("CHECKING_AVAILABILITY_A", e.toState());
    }

    @Test
    void coordinationCompleted_instantiation() {
        var e = CoordinationCompleted.of(CoordinationId.generate(),
                EventId.of("a"), EventId.of("b"));
        assertNotNull(e.eventIdA());
        assertNotNull(e.eventIdB());
    }

    @Test
    void coordinationFailed_instantiation() {
        var e = CoordinationFailed.of(CoordinationId.generate(), "No availability");
        assertEquals("No availability", e.reason());
    }

    @Test
    void approvalDecisionMade_instantiation() {
        var e = ApprovalDecisionMade.of(ApprovalId.generate(), UserId.generate(), "APPROVED", "PERSONAL");
        assertEquals("APPROVED", e.decision());
        assertEquals("PERSONAL", e.approvalType());
    }

    @Test
    void approvalExpired_instantiation() {
        var e = ApprovalExpired.of(ApprovalId.generate(), UserId.generate(), "COLLABORATIVE", Instant.now());
        assertEquals("COLLABORATIVE", e.approvalType());
    }

    @Test
    void allEvents_haveEventId() {
        var e = UserRegistered.of(UserId.generate(), "bob", Email.of("b@c.com"),
                SlackUserId.of("U2"), WorkspaceId.of("T2"));
        assertNotNull(e.eventId()); // default UUID from DomainEvent interface
    }
}
