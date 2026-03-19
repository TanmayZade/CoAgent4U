package com.coagent4u.coordination.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.common.DomainEventPublisher;

import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.coordination.domain.AvailabilityResult;
import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.domain.CoordinationStateLogEntry;
import com.coagent4u.coordination.domain.EventCreationSaga;
import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.coordination.domain.ProposalGenerator;
import com.coagent4u.coordination.domain.SlotGenerator;
import com.coagent4u.coordination.domain.SlotMatcher;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.coordination.port.out.ApprovalRequestResult;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Application service for the Coordination bounded context.
 * Implements {@link CoordinationProtocolPort}.
 *
 * <p>
 * Orchestrates the full coordination lifecycle: initiation → availability →
 * slot generation → slot matching → slot selection → approval → event creation
 * → completion.
 *
 * <p>
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class CoordinationService implements CoordinationProtocolPort {

    private static final Logger log = LoggerFactory.getLogger(CoordinationService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final CoordinationPersistencePort persistence;
    private final AgentAvailabilityPort agentAvailabilityPort;
    private final AgentEventExecutionPort agentEventExecutionPort;
    private final AgentProfilePort agentProfilePort;
    private final AgentApprovalPort agentApprovalPort;
    private final DomainEventPublisher eventPublisher;

    private final SlotGenerator slotGenerator = new SlotGenerator();
    private final SlotMatcher slotMatcher = new SlotMatcher();
    private final ProposalGenerator proposalGenerator = new ProposalGenerator();
    private final EventCreationSaga eventCreationSaga = new EventCreationSaga();

    public CoordinationService(CoordinationPersistencePort persistence,
            AgentAvailabilityPort agentAvailabilityPort,
            AgentEventExecutionPort agentEventExecutionPort,
            AgentProfilePort agentProfilePort,
            AgentApprovalPort agentApprovalPort,
            DomainEventPublisher eventPublisher) {
        this.persistence = persistence;
        this.agentAvailabilityPort = agentAvailabilityPort;
        this.agentEventExecutionPort = agentEventExecutionPort;
        this.agentProfilePort = agentProfilePort;
        this.agentApprovalPort = agentApprovalPort;
        this.eventPublisher = eventPublisher;
    }

    // ── Step 1–5: Initiation through Slot Generation ──

    @Override
    public CoordinationId initiate(CoordinationId coordId, com.coagent4u.shared.CorrelationId correlationId, AgentId requesterAgentId, AgentId inviteeAgentId,
            TimeRange lookAheadRange, int durationMinutes,
            String title, String timezone) {

        Coordination coordination = new Coordination(coordId, requesterAgentId, inviteeAgentId);
        coordination.setMetadata("correlationId", correlationId.value().toString());
        persistence.save(coordination);
        log.info("[CoordinationService] Coordination {} created → INITIATED", coordId);

        com.coagent4u.shared.UserId requesterUserId = agentProfilePort.getProfile(requesterAgentId).userId();
        com.coagent4u.shared.UserId inviteeUserId = agentProfilePort.getProfile(inviteeAgentId).userId();

        // Step 2: Check requester availability
        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "Checking requester availability");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → CHECKING_AVAILABILITY_A", coordId);
        AvailabilityResult availA = agentAvailabilityPort.getAvailability(requesterAgentId, lookAheadRange);
        List<AvailabilityBlock> freeA = availA.freeBlocks();
        log.info("[CoordinationService] Retrieved {} free blocks ({} busy slots) for requester agent {}",
                freeA.size(), availA.busyCount(), requesterAgentId);
        eventPublisher.publish(com.coagent4u.common.events.CalendarSourced.of(requesterAgentId, requesterUserId, correlationId, coordId, availA.busyCount()));
        eventPublisher.publish(com.coagent4u.common.events.SlotsProposed.of(requesterAgentId, requesterUserId, correlationId, coordId, availA.busyCount()));

        // Step 3: Check invitee availability
        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "Checking invitee availability");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → CHECKING_AVAILABILITY_B", coordId);
        AvailabilityResult availB = agentAvailabilityPort.getAvailability(inviteeAgentId, lookAheadRange);
        List<AvailabilityBlock> freeB = availB.freeBlocks();
        log.info("[CoordinationService] Retrieved {} free blocks ({} busy slots) for invitee agent {}",
                freeB.size(), availB.busyCount(), inviteeAgentId);
        eventPublisher.publish(com.coagent4u.common.events.CalendarSourced.of(inviteeAgentId, inviteeUserId, correlationId, coordId, availB.busyCount()));
        eventPublisher.publish(com.coagent4u.common.events.SlotsProposed.of(inviteeAgentId, inviteeUserId, correlationId, coordId, availB.busyCount()));

        // Step 4: Matching — generate office-hour slots and remove busy ones
        coordination.transition(CoordinationState.MATCHING, "Matching availability");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → MATCHING", coordId);

        // Generate office-hour slots for each day in lookAheadRange
        LocalDate startDate = lookAheadRange.start();
        LocalDate endDate = lookAheadRange.end();
        ZoneId tz = timezone != null ? ZoneId.of(timezone) : IST;

        List<TimeSlot> allOfficeSlots = new java.util.ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            allOfficeSlots.addAll(slotGenerator.generateSlots(
                    date, SlotGenerator.DEFAULT_OFFICE_START, SlotGenerator.DEFAULT_OFFICE_END,
                    durationMinutes, tz));
        }
        log.info("[SlotGenerator] Generated {} total office-hour slots for {} to {}", allOfficeSlots.size(), startDate,
                endDate);

        List<TimeSlot> matched = slotMatcher.matchSlots(allOfficeSlots, freeA, freeB);

        if (matched.isEmpty()) {
            coordination.transition(CoordinationState.FAILED, "No available slots found");
            persistence.save(coordination);
            eventPublisher.publish(com.coagent4u.common.events.ConflictDetected.of(requesterAgentId, requesterUserId, correlationId, coordId, "No overlapping available slots found between profiles"));
            eventPublisher.publish(com.coagent4u.common.events.ConflictDetected.of(inviteeAgentId, inviteeUserId, correlationId, coordId, "No overlapping available slots found between profiles"));
            log.info("[CoordinationService] {} → FAILED (no available slots)", coordId);
            return coordId;
        }

        // Step 5: Store available slots and transition to PROPOSAL_GENERATED
        coordination.setAvailableSlots(matched);
        coordination.transition(CoordinationState.PROPOSAL_GENERATED,
                "Proposal generated with " + matched.size() + " available slots");
        persistence.save(coordination);
        eventPublisher.publish(com.coagent4u.common.events.SlotsReceived.of(requesterAgentId, requesterUserId, correlationId, coordId, matched.size()));
        eventPublisher.publish(com.coagent4u.common.events.SlotsReceived.of(inviteeAgentId, inviteeUserId, correlationId, coordId, matched.size()));
        log.info("[CoordinationService] {} → PROPOSAL_GENERATED ({} slots available)", coordId, matched.size());

        return coordId;
    }

    // ── Step 5b: Get available slots (for agent module to send to Slack) ──

    @Override
    public List<TimeSlot> getAvailableSlots(CoordinationId coordinationId) {
        Coordination coordination = load(coordinationId);
        return coordination.getAvailableSlots();
    }

    // ── Step 6: Slot Selection by User B ──

    @Override
    public void selectSlot(CoordinationId coordinationId, TimeSlot selectedSlot) {
        log.info("[CoordinationService] Slot selection for {}: {}", coordinationId, selectedSlot);
        Coordination coordination = load(coordinationId);

        // Idempotency guard: only process if still in PROPOSAL_GENERATED
        if (coordination.getState() != CoordinationState.PROPOSAL_GENERATED) {
            log.info("[CoordinationService] {} Slot already selected (state={}), ignoring duplicate",
                    coordinationId, coordination.getState());
            return;
        }

        coordination.selectSlot(selectedSlot);
        log.info("[CoordinationService] {} selectedSlot={}", coordinationId, selectedSlot);

        // Generate proposal from selected slot
        MeetingProposal proposal = proposalGenerator.generate(
                coordinationId.value().toString(),
                coordination.getRequesterAgentId(),
                coordination.getInviteeAgentId(),
                selectedSlot,
                (int) java.time.Duration.between(selectedSlot.start(), selectedSlot.end()).toMinutes(),
                "Meeting",
                IST.getId());
        coordination.setProposal(proposal);

        // Invitee's slot selection (with Reject option) counts as their approval.
        // Skip AWAITING_APPROVAL_B → go directly to AWAITING_APPROVAL_A (requester confirmation).
        coordination.transition(CoordinationState.AWAITING_APPROVAL_A, "Invitee approved via slot selection, awaiting requester");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → AWAITING_APPROVAL_A (invitee approved via slot selection)", coordinationId);

        ApprovalRequestResult result = agentApprovalPort.requestApproval(coordination.getRequesterAgentId(), proposal);
        log.info("[ApprovalService] Approval created id={} for requester agent {}", result.approvalId(),
                coordination.getRequesterAgentId());

        if (result.messageTs() != null) {
            coordination.setMetadata("requester_approval_ts", result.messageTs());
        }

        persistence.save(coordination);
    }

    // ── Steps 7–9: Approval Handling ──

    @Override
    public void handleApproval(CoordinationId coordinationId, AgentId agentId, boolean approved) {
        log.info("[CoordinationService] Approval decision for {}: agent={} approved={}", coordinationId, agentId,
                approved);
        Coordination coordination = load(coordinationId);
        String corrIdStr = coordination.getMetadata("correlationId");
        com.coagent4u.shared.CorrelationId correlationId = corrIdStr != null ? new com.coagent4u.shared.CorrelationId(java.util.UUID.fromString(corrIdStr)) : com.coagent4u.shared.CorrelationId.generate();

        if (!approved) {
            String rejectReason = "REJECTED_BY_AGENT:" + agentId.value();
            coordination.transition(CoordinationState.REJECTED, rejectReason);
            persistence.save(coordination);
            log.info("[CoordinationService] {} → REJECTED by agent {}", coordinationId, agentId);

            com.coagent4u.shared.UserId requesterUserId = agentProfilePort.getProfile(coordination.getRequesterAgentId()).userId();
            com.coagent4u.shared.UserId inviteeUserId = agentProfilePort.getProfile(coordination.getInviteeAgentId()).userId();
            eventPublisher.publish(com.coagent4u.common.events.CoordinationRejected.of(coordination.getRequesterAgentId(), requesterUserId, correlationId, coordinationId, rejectReason));
            eventPublisher.publish(com.coagent4u.common.events.CoordinationRejected.of(coordination.getInviteeAgentId(), inviteeUserId, correlationId, coordinationId, rejectReason));
            return;
        }

        CoordinationState currentState = coordination.getState();

        if (currentState == CoordinationState.AWAITING_APPROVAL_B) {
            // Invitee approved → now request requester approval
            coordination.transition(CoordinationState.AWAITING_APPROVAL_A, "Invitee approved, awaiting requester");
            persistence.save(coordination);
            log.info("[CoordinationService] {} → AWAITING_APPROVAL_A (invitee approved)", coordinationId);

            ApprovalRequestResult result = agentApprovalPort.requestApproval(
                    coordination.getRequesterAgentId(), coordination.getProposal());
            log.info("[ApprovalService] Approval created id={} for requester agent {}", result.approvalId(),
                    coordination.getRequesterAgentId());

            if (result.messageTs() != null) {
                coordination.setMetadata("requester_approval_ts", result.messageTs());
            }

            persistence.save(coordination);

        } else if (currentState == CoordinationState.AWAITING_APPROVAL_A) {
            // Requester approved → both approved → execute event creation saga
            coordination.transition(CoordinationState.APPROVED_BY_BOTH, "Both parties approved");
            persistence.save(coordination);
            log.info("[CoordinationService] {} → APPROVED_BY_BOTH", coordinationId);

            log.info("[CoordinationService] {} Executing event creation saga...", coordinationId);
            com.coagent4u.coordination.domain.EventCreationSaga.SagaResult result = eventCreationSaga.execute(coordination, agentEventExecutionPort);
            persistence.save(coordination);

            if (result.success()) {
                log.info("[CoordinationService] {} → COMPLETED (both events created)", coordinationId);


                com.coagent4u.shared.UserId requesterUserId = agentProfilePort.getProfile(coordination.getRequesterAgentId()).userId();
                com.coagent4u.shared.UserId inviteeUserId = agentProfilePort.getProfile(coordination.getInviteeAgentId()).userId();

                eventPublisher.publish(com.coagent4u.common.events.CalendarEventCreated.of(coordination.getRequesterAgentId(), requesterUserId, correlationId, coordinationId, result.eventIdA()));
                eventPublisher.publish(com.coagent4u.common.events.CalendarEventCreated.of(coordination.getInviteeAgentId(), inviteeUserId, correlationId, coordinationId, result.eventIdB()));
                
                eventPublisher.publish(com.coagent4u.common.events.CoordinationCompleted.of(coordinationId, result.eventIdA(), result.eventIdB()));
            } else {
                log.error("[CoordinationService] {} → FAILED (event creation saga failed)", coordinationId);
                eventPublisher.publish(com.coagent4u.common.events.CoordinationFailed.of(coordinationId, "Event creation saga failed"));
            }
        }
    }

    // ── Legacy methods ──

    @Override
    public void advance(CoordinationId coordinationId, CoordinationState toState, String reason) {
        log.info("[CoordinationService] ADVANCING {} → {} | Reason: {}", coordinationId, toState, reason);
        Coordination coordination = load(coordinationId);
        coordination.transition(toState, reason);
        persistence.save(coordination);

        if (toState == CoordinationState.APPROVED_BY_BOTH) {
            log.info("[CoordinationService] {} APPROVED BY BOTH — executing event creation saga", coordinationId);
            eventCreationSaga.execute(coordination, agentEventExecutionPort);
            persistence.save(coordination);
        }
    }

    @Override
    public void updateMetadata(CoordinationId coordinationId, String key, String value) {
        log.info("[CoordinationService] Updating metadata for {}: {}={}", coordinationId, key, value);
        Coordination coordination = load(coordinationId);
        coordination.setMetadata(key, value);
        persistence.save(coordination);
    }

    @Override
    public String getMetadata(CoordinationId coordinationId, String key) {
        Coordination coordination = load(coordinationId);
        return coordination.getMetadata(key);
    }

    @Override
    public void terminate(CoordinationId coordinationId, String reason) {
        log.warn("[CoordinationService] TERMINATING {} | Reason: {}", coordinationId, reason);
        Coordination coordination = load(coordinationId);
        if (!coordination.isTerminal()) {
            coordination.transition(CoordinationState.FAILED, reason);
            persistence.save(coordination);
            eventPublisher.publish(com.coagent4u.common.events.CoordinationFailed.of(coordinationId, reason));
        }
    }

    private Coordination load(CoordinationId id) {
        return persistence.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coordination not found: " + id));
    }

}
