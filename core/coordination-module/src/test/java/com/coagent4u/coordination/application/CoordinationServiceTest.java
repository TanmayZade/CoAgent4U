package com.coagent4u.coordination.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.UserId;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoordinationService Tests")
class CoordinationServiceTest {

        @Mock
        private CoordinationPersistencePort persistence;
        @Mock
        private AgentAvailabilityPort agentAvailabilityPort;
        @Mock
        private AgentEventExecutionPort agentEventExecutionPort;
        @Mock
        private AgentProfilePort agentProfilePort;
        @Mock
        private AgentApprovalPort agentApprovalPort;
        @Mock
        private DomainEventPublisher eventPublisher;

        private CoordinationService service;

        @BeforeEach
        void setUp() {
                service = new CoordinationService(
                                persistence, agentAvailabilityPort, agentEventExecutionPort,
                                agentProfilePort, agentApprovalPort, eventPublisher);
        }

        @Test
        @DisplayName("initiate: happy path reaches AWAITING_APPROVAL_B")
        void initiate_happyPath() {
                AgentId requester = AgentId.generate();
                AgentId invitee = AgentId.generate();
                TimeRange range = TimeRange.of(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 7));

                // Mock availability with overlapping blocks
                List<AvailabilityBlock> blocksA = List.of(new AvailabilityBlock(
                                Instant.parse("2026-03-05T10:00:00Z"), Instant.parse("2026-03-05T12:00:00Z")));
                List<AvailabilityBlock> blocksB = List.of(new AvailabilityBlock(
                                Instant.parse("2026-03-05T10:30:00Z"), Instant.parse("2026-03-05T13:00:00Z")));

                when(agentAvailabilityPort.getAvailability(eq(requester), any())).thenReturn(blocksA);
                when(agentAvailabilityPort.getAvailability(eq(invitee), any())).thenReturn(blocksB);
                when(agentProfilePort.getProfile(requester))
                                .thenReturn(new AgentProfilePort.AgentProfile(
                                                requester, new UserId(java.util.UUID.randomUUID()), "Alice", "UTC"));
                when(agentApprovalPort.requestApproval(any(), any()))
                                .thenReturn(new ApprovalId(java.util.UUID.randomUUID()));

                CoordinationId result = service.initiate(requester, invitee, range, 30, "Sync", "UTC");

                assertNotNull(result);
                verify(persistence, atLeast(5)).save(any(Coordination.class));
                verify(agentApprovalPort).requestApproval(eq(invitee), any());
                verify(eventPublisher, atLeastOnce()).publish(any());
        }

        @Test
        @DisplayName("initiate: no overlap transitions to FAILED")
        void initiate_noOverlap_fails() {
                AgentId requester = AgentId.generate();
                AgentId invitee = AgentId.generate();
                TimeRange range = TimeRange.of(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 7));

                List<AvailabilityBlock> blocksA = List.of(new AvailabilityBlock(
                                Instant.parse("2026-03-05T09:00:00Z"), Instant.parse("2026-03-05T10:00:00Z")));
                List<AvailabilityBlock> blocksB = List.of(new AvailabilityBlock(
                                Instant.parse("2026-03-05T11:00:00Z"), Instant.parse("2026-03-05T12:00:00Z")));

                when(agentAvailabilityPort.getAvailability(eq(requester), any())).thenReturn(blocksA);
                when(agentAvailabilityPort.getAvailability(eq(invitee), any())).thenReturn(blocksB);

                CoordinationId result = service.initiate(requester, invitee, range, 30, "Sync", "UTC");

                assertNotNull(result);
                verify(agentApprovalPort, never()).requestApproval(any(), any());
        }

        @Test
        @DisplayName("advance to APPROVED_BY_BOTH triggers EventCreationSaga")
        void advance_approvedByBoth_triggersSaga() {
                AgentId requester = AgentId.generate();
                AgentId invitee = AgentId.generate();
                CoordinationId coordId = CoordinationId.generate();

                // Create a coordination in AWAITING_APPROVAL_A state
                Coordination coordination = new Coordination(coordId, requester, invitee);
                coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "A");
                coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "B");
                coordination.transition(CoordinationState.MATCHING, "Match");
                coordination.transition(CoordinationState.PROPOSAL_GENERATED, "Proposal");
                coordination.transition(CoordinationState.AWAITING_APPROVAL_B, "Await B");
                coordination.transition(CoordinationState.AWAITING_APPROVAL_A, "Await A");

                // Set proposal for the saga
                coordination.setProposal(new com.coagent4u.coordination.domain.MeetingProposal(
                                "prop-1", coordId.value().toString(), requester, invitee,
                                new com.coagent4u.shared.TimeSlot(
                                                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400)),
                                30, "Test Meeting", "UTC"));

                when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));
                when(agentEventExecutionPort.createEvent(any(), any(), any()))
                                .thenReturn(new EventId("evt-1"));

                service.advance(coordId, CoordinationState.APPROVED_BY_BOTH, "Both approved");

                assertEquals(CoordinationState.COMPLETED, coordination.getState());
                verify(agentEventExecutionPort, times(2)).createEvent(any(), any(), any());
        }

        @Test
        @DisplayName("terminate transitions non-terminal coordination to FAILED")
        void terminate_nonTerminal_transitionsToFailed() {
                CoordinationId coordId = CoordinationId.generate();
                Coordination coordination = new Coordination(coordId, AgentId.generate(), AgentId.generate());
                coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "A");

                when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

                service.terminate(coordId, "User cancelled");

                assertEquals(CoordinationState.FAILED, coordination.getState());
                verify(persistence).save(coordination);
        }

        @Test
        @DisplayName("terminate on already-terminal coordination is a no-op")
        void terminate_alreadyTerminal_noOp() {
                CoordinationId coordId = CoordinationId.generate();
                Coordination coordination = new Coordination(coordId, AgentId.generate(), AgentId.generate());
                coordination.transition(CoordinationState.FAILED, "Earlier failure");

                when(persistence.findById(coordId)).thenReturn(Optional.of(coordination));

                service.terminate(coordId, "Try again");

                assertEquals(CoordinationState.FAILED, coordination.getState());
                // save should NOT be called again (no state change)
                verify(persistence, never()).save(coordination);
        }
}
