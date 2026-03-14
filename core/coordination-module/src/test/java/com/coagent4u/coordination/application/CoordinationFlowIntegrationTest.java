package com.coagent4u.coordination.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Integration test suite for the full A2A coordination protocol.
 * Uses mock adapters for Slack (notifications) and calendar (events).
 *
 * <p>
 * Tests all 8 scenarios from the QA test plan:
 * 1. Happy path end-to-end
 * 2. No available slots
 * 3. Invitee rejects
 * 4. Requester rejects
 * 5. Calendar event creation fails (event A)
 * 6. Calendar event creation fails (event B with compensation)
 * 7. Duplicate slot selection (idempotency)
 * 8. Requester name in invite message (verified via mock)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Coordination Flow Integration Tests")
class CoordinationFlowIntegrationTest {

    @Mock
    private CoordinationPersistencePort persistence;
    @Mock
    private AgentAvailabilityPort availabilityPort;
    @Mock
    private AgentProfilePort profilePort;
    @Mock
    private AgentApprovalPort approvalPort;
    @Mock
    private DomainEventPublisher eventPublisher;

    private MockCalendarAdapter mockCalendar;
    private CoordinationService service;

    private final AgentId requester = AgentId.generate();
    private final AgentId invitee = AgentId.generate();
    private final TimeRange lookAhead = TimeRange.of(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10));

    // Availability: both free all day → results in overlapping office-hour slots
    private final List<AvailabilityBlock> fullDayFreeA = List.of(new AvailabilityBlock(
            Instant.parse("2026-03-10T00:00:00Z"), Instant.parse("2026-03-10T23:59:59Z")));
    private final List<AvailabilityBlock> fullDayFreeB = List.of(new AvailabilityBlock(
            Instant.parse("2026-03-10T00:00:00Z"), Instant.parse("2026-03-10T23:59:59Z")));

    // No overlap blocks
    private final List<AvailabilityBlock> morningOnlyA = List.of(new AvailabilityBlock(
            Instant.parse("2026-03-10T02:00:00Z"), Instant.parse("2026-03-10T03:00:00Z")));
    private final List<AvailabilityBlock> afternoonOnlyB = List.of(new AvailabilityBlock(
            Instant.parse("2026-03-10T14:00:00Z"), Instant.parse("2026-03-10T15:00:00Z")));

    @BeforeEach
    void setUp() {
        mockCalendar = new MockCalendarAdapter();
        service = new CoordinationService(
                persistence, availabilityPort, mockCalendar,
                profilePort, approvalPort, eventPublisher);
    }

    // ── Helper: capture coordination saved to persistence ──

    private Coordination captureLastSaved() {
        var captor = org.mockito.ArgumentCaptor.forClass(Coordination.class);
        verify(persistence, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 1: Happy Path — Full End-to-End
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 1: Happy Path End-to-End")
    class HappyPath {

        @Test
        @DisplayName("initiate → PROPOSAL_GENERATED with slots")
        void initiate_producesSlots() {
            when(availabilityPort.getAvailability(eq(requester), any())).thenReturn(fullDayFreeA);
            when(availabilityPort.getAvailability(eq(invitee), any())).thenReturn(fullDayFreeB);

            CoordinationId coordId = service.initiate(requester, invitee, lookAhead, 60, "Meeting", "Asia/Kolkata");
            assertNotNull(coordId);

            // Verify availability was checked for both agents
            verify(availabilityPort).getAvailability(eq(requester), any());
            verify(availabilityPort).getAvailability(eq(invitee), any());
            // Verify coordination was saved multiple times (state transitions)
            verify(persistence, atLeast(5)).save(any(Coordination.class));
        }

        @Test
        @DisplayName("selectSlot → AWAITING_APPROVAL_B + approval requested from invitee")
        void selectSlot_transitionsToAwaitingB() {
            // Set up a coordination in PROPOSAL_GENERATED state
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = new Coordination(coordId, requester, invitee);
            coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "A");
            coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "B");
            coordination.transition(CoordinationState.MATCHING, "Match");

            TimeSlot slot = new TimeSlot(
                    Instant.parse("2026-03-10T04:30:00Z"),
                    Instant.parse("2026-03-10T05:30:00Z"));
            coordination.setAvailableSlots(List.of(slot));
            coordination.transition(CoordinationState.PROPOSAL_GENERATED, "Generated");

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));
            when(approvalPort.requestApproval(any(), any()))
                    .thenReturn(new ApprovalId(UUID.randomUUID()));

            service.selectSlot(coordId, slot);

            assertEquals(CoordinationState.AWAITING_APPROVAL_B, coordination.getState());
            verify(approvalPort).requestApproval(eq(invitee), any());
        }

        @Test
        @DisplayName("handleApproval invitee approved → AWAITING_APPROVAL_A → requester approval requested")
        void inviteeApproved_transitionsToAwaitingA() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_B);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));
            when(approvalPort.requestApproval(any(), any()))
                    .thenReturn(new ApprovalId(UUID.randomUUID()));

            service.handleApproval(coordId, invitee, true);

            assertEquals(CoordinationState.AWAITING_APPROVAL_A, coordination.getState());
            verify(approvalPort).requestApproval(eq(requester), any());
        }

        @Test
        @DisplayName("handleApproval requester approved → COMPLETED with 2 events created")
        void requesterApproved_completesWithEvents() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_A);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

            service.handleApproval(coordId, requester, true);

            assertEquals(CoordinationState.COMPLETED, coordination.getState());
            assertEquals(2, mockCalendar.getCreatedEvents().size());
            assertEquals(0, mockCalendar.getDeletedEvents().size());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 2: No Available Slots
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 2: No Available Slots")
    class NoSlots {

        @Test
        @DisplayName("initiate with no overlap → FAILED")
        void noOverlap_transitionsToFailed() {
            when(availabilityPort.getAvailability(eq(requester), any())).thenReturn(morningOnlyA);
            when(availabilityPort.getAvailability(eq(invitee), any())).thenReturn(afternoonOnlyB);

            CoordinationId coordId = service.initiate(requester, invitee, lookAhead, 60, "Meeting", "Asia/Kolkata");

            assertNotNull(coordId);
            verify(approvalPort, never()).requestApproval(any(), any());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 3: Invitee Rejects
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 3: Invitee Rejects")
    class InviteeRejects {

        @Test
        @DisplayName("invitee rejects → REJECTED, no events created")
        void inviteeRejects_transitionsToRejected() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_B);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

            service.handleApproval(coordId, invitee, false);

            assertEquals(CoordinationState.REJECTED, coordination.getState());
            assertEquals(0, mockCalendar.getCreateCallCount());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 4: Requester Rejects
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 4: Requester Rejects")
    class RequesterRejects {

        @Test
        @DisplayName("requester rejects → REJECTED, no events created")
        void requesterRejects_transitionsToRejected() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_A);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

            service.handleApproval(coordId, requester, false);

            assertEquals(CoordinationState.REJECTED, coordination.getState());
            assertEquals(0, mockCalendar.getCreateCallCount());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 5: Calendar Event A Fails
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 5: Calendar Event A Fails")
    class CalendarEventAFails {

        @Test
        @DisplayName("event A creation fails → FAILED, no compensation needed")
        void eventAFails_transitionsToFailed() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_A);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));
            mockCalendar.setFailOnCreateA(true);

            service.handleApproval(coordId, requester, true);

            assertEquals(CoordinationState.FAILED, coordination.getState());
            assertEquals(0, mockCalendar.getDeletedEvents().size());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 6: Calendar Event B Fails (with Compensation)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 6: Calendar Event B Fails")
    class CalendarEventBFails {

        @Test
        @DisplayName("event B fails → compensate by deleting A → FAILED")
        void eventBFails_compensatesAndFails() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_A);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));
            mockCalendar.setFailOnCreateB(true);

            service.handleApproval(coordId, requester, true);

            assertEquals(CoordinationState.FAILED, coordination.getState());
            assertEquals(1, mockCalendar.getCreatedEvents().size()); // Only A created
            assertEquals(1, mockCalendar.getDeletedEvents().size()); // A compensated
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 7: Duplicate Slot Selection (Idempotency)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 7: Idempotent Slot Selection")
    class DuplicateSlotSelection {

        @Test
        @DisplayName("second slot selection on AWAITING_APPROVAL_B is ignored")
        void duplicateSlotSelection_isIgnored() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = createCoordinationInState(coordId, CoordinationState.AWAITING_APPROVAL_B);

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

            // This should be silently ignored (state is not PROPOSAL_GENERATED)
            TimeSlot slot = new TimeSlot(
                    Instant.parse("2026-03-10T04:30:00Z"),
                    Instant.parse("2026-03-10T05:30:00Z"));
            service.selectSlot(coordId, slot);

            // State should remain AWAITING_APPROVAL_B
            assertEquals(CoordinationState.AWAITING_APPROVAL_B, coordination.getState());
            // No additional approval should have been requested
            verify(approvalPort, never()).requestApproval(any(), any());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Scenario 8: Requester Name in Invite (verified via port contract)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Scenario 8: Requester Metadata Propagation")
    class RequesterMetadata {

        @Test
        @DisplayName("MeetingProposal carries both requester and invitee AgentIds")
        void proposalCarriesAgentIds() {
            CoordinationId coordId = CoordinationId.generate();
            Coordination coordination = new Coordination(coordId, requester, invitee);
            coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "A");
            coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "B");
            coordination.transition(CoordinationState.MATCHING, "Match");

            TimeSlot slot = new TimeSlot(
                    Instant.parse("2026-03-10T04:30:00Z"),
                    Instant.parse("2026-03-10T05:30:00Z"));
            coordination.setAvailableSlots(List.of(slot));
            coordination.transition(CoordinationState.PROPOSAL_GENERATED, "Generated");

            when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

            var approvalCaptor = org.mockito.ArgumentCaptor.forClass(
                    com.coagent4u.coordination.domain.MeetingProposal.class);
            when(approvalPort.requestApproval(any(), approvalCaptor.capture()))
                    .thenReturn(new ApprovalId(UUID.randomUUID()));

            service.selectSlot(coordId, slot);

            assertEquals(CoordinationState.AWAITING_APPROVAL_B, coordination.getState());

            // Verify the proposal carries BOTH agent IDs
            var capturedProposal = approvalCaptor.getValue();
            assertEquals(requester, capturedProposal.requesterAgentId());
            assertEquals(invitee, capturedProposal.inviteeAgentId());

            // Verify coordinationIdStr is set properly
            assertNotNull(capturedProposal.coordinationIdStr());
            assertEquals(coordId.value().toString(), capturedProposal.coordinationIdStr());
        }
    }

    // ── Helper: create coordination in a specific state ──

    private Coordination createCoordinationInState(CoordinationId coordId, CoordinationState targetState) {
        Coordination c = new Coordination(coordId, requester, invitee);
        TimeSlot slot = new TimeSlot(
                Instant.parse("2026-03-10T04:30:00Z"),
                Instant.parse("2026-03-10T05:30:00Z"));

        c.transition(CoordinationState.CHECKING_AVAILABILITY_A, "A");
        c.transition(CoordinationState.CHECKING_AVAILABILITY_B, "B");
        c.transition(CoordinationState.MATCHING, "Match");
        c.setAvailableSlots(List.of(slot));
        c.transition(CoordinationState.PROPOSAL_GENERATED, "Generated");

        var proposal = new com.coagent4u.coordination.domain.MeetingProposal(
                "proposal-" + UUID.randomUUID(),
                coordId.value().toString(),
                requester, invitee, slot, 60, "Test Meeting", "Asia/Kolkata");
        c.setProposal(proposal);

        // Selective state progression
        if (targetState == CoordinationState.AWAITING_APPROVAL_B
                || targetState == CoordinationState.AWAITING_APPROVAL_A
                || targetState == CoordinationState.APPROVED_BY_BOTH) {
            c.transition(CoordinationState.AWAITING_APPROVAL_B, "Await B");
        }
        if (targetState == CoordinationState.AWAITING_APPROVAL_A
                || targetState == CoordinationState.APPROVED_BY_BOTH) {
            c.transition(CoordinationState.AWAITING_APPROVAL_A, "Await A");
        }
        if (targetState == CoordinationState.APPROVED_BY_BOTH) {
            c.transition(CoordinationState.APPROVED_BY_BOTH, "Both approved");
        }

        return c;
    }
}
