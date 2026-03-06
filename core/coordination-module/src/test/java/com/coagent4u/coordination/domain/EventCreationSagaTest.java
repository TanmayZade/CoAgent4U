package com.coagent4u.coordination.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeSlot;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventCreationSaga Tests")
class EventCreationSagaTest {

    @Mock
    private AgentEventExecutionPort agentEventExec;

    private final EventCreationSaga saga = new EventCreationSaga();

    private Coordination createApprovedCoordination() {
        AgentId requester = AgentId.generate();
        AgentId invitee = AgentId.generate();
        Coordination c = new Coordination(CoordinationId.generate(), requester, invitee);
        TimeSlot slot = new TimeSlot(Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400));
        MeetingProposal proposal = new MeetingProposal(
                "proposal-" + UUID.randomUUID(), requester, invitee,
                slot, 30, "Sync Meeting", "UTC");
        c.setProposal(proposal);
        // Transition to APPROVED_BY_BOTH
        c.transition(CoordinationState.CHECKING_AVAILABILITY_A, "Check A");
        c.transition(CoordinationState.CHECKING_AVAILABILITY_B, "Check B");
        c.transition(CoordinationState.MATCHING, "Matching");
        c.transition(CoordinationState.PROPOSAL_GENERATED, "Generated");
        c.transition(CoordinationState.AWAITING_APPROVAL_B, "Await B");
        c.transition(CoordinationState.AWAITING_APPROVAL_A, "Await A");
        c.transition(CoordinationState.APPROVED_BY_BOTH, "Both approved");
        return c;
    }

    @Test
    @DisplayName("Happy path: both events created → COMPLETED")
    void happyPath_bothEventsCreated() {
        Coordination c = createApprovedCoordination();
        EventId eventA = new EventId("event-a");
        EventId eventB = new EventId("event-b");

        when(agentEventExec.createEvent(eq(c.getRequesterAgentId()), any(), any())).thenReturn(eventA);
        when(agentEventExec.createEvent(eq(c.getInviteeAgentId()), any(), any())).thenReturn(eventB);

        boolean result = saga.execute(c, agentEventExec);

        assertTrue(result);
        assertEquals(CoordinationState.COMPLETED, c.getState());
        verify(agentEventExec, times(2)).createEvent(any(), any(), any());
        verify(agentEventExec, never()).deleteEvent(any(), any());
    }

    @Test
    @DisplayName("Event A creation fails → FAILED (no compensation needed)")
    void eventAFails_transitionsToFailed() {
        Coordination c = createApprovedCoordination();

        when(agentEventExec.createEvent(eq(c.getRequesterAgentId()), any(), any()))
                .thenThrow(new RuntimeException("Calendar unavailable"));

        boolean result = saga.execute(c, agentEventExec);

        assertFalse(result);
        assertEquals(CoordinationState.FAILED, c.getState());
        verify(agentEventExec, times(1)).createEvent(any(), any(), any());
        verify(agentEventExec, never()).deleteEvent(any(), any()); // No compensation needed
    }

    @Test
    @DisplayName("Event B fails → compensates by deleting Event A → FAILED")
    void eventBFails_compensatesAndFails() {
        Coordination c = createApprovedCoordination();
        EventId eventA = new EventId("event-a");

        when(agentEventExec.createEvent(eq(c.getRequesterAgentId()), any(), any())).thenReturn(eventA);
        when(agentEventExec.createEvent(eq(c.getInviteeAgentId()), any(), any()))
                .thenThrow(new RuntimeException("Invitee calendar error"));

        boolean result = saga.execute(c, agentEventExec);

        assertFalse(result);
        assertEquals(CoordinationState.FAILED, c.getState());
        // Verify compensation: Event A was deleted
        verify(agentEventExec).deleteEvent(c.getRequesterAgentId(), eventA);
    }

    @Test
    @DisplayName("Event B fails, compensation also fails → still FAILED")
    void eventBFails_compensationAlsoFails_stillFailed() {
        Coordination c = createApprovedCoordination();
        EventId eventA = new EventId("event-a");

        when(agentEventExec.createEvent(eq(c.getRequesterAgentId()), any(), any())).thenReturn(eventA);
        when(agentEventExec.createEvent(eq(c.getInviteeAgentId()), any(), any()))
                .thenThrow(new RuntimeException("Invitee error"));
        doThrow(new RuntimeException("Compensation failed"))
                .when(agentEventExec).deleteEvent(any(), any());

        boolean result = saga.execute(c, agentEventExec);

        assertFalse(result);
        assertEquals(CoordinationState.FAILED, c.getState());
    }
}
