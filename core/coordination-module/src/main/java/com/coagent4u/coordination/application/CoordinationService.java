package com.coagent4u.coordination.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.common.events.CoordinationStateChanged;
import com.coagent4u.coordination.domain.AvailabilityBlock;
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
    public CoordinationId initiate(AgentId requesterAgentId, AgentId inviteeAgentId,
            TimeRange lookAheadRange, int durationMinutes,
            String title, String timezone) {

        CoordinationId coordId = CoordinationId.generate();
        Coordination coordination = new Coordination(coordId, requesterAgentId, inviteeAgentId);
        persistence.save(coordination);
        log.info("[CoordinationService] Coordination {} created → INITIATED", coordId);

        // Step 2: Check requester availability
        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "Checking requester availability");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → CHECKING_AVAILABILITY_A", coordId);
        List<AvailabilityBlock> freeA = agentAvailabilityPort.getAvailability(requesterAgentId, lookAheadRange);
        log.info("[CoordinationService] Retrieved {} free blocks for requester agent {}", freeA.size(),
                requesterAgentId);

        // Step 3: Check invitee availability
        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "Checking invitee availability");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → CHECKING_AVAILABILITY_B", coordId);
        List<AvailabilityBlock> freeB = agentAvailabilityPort.getAvailability(inviteeAgentId, lookAheadRange);
        log.info("[CoordinationService] Retrieved {} free blocks for invitee agent {}", freeB.size(), inviteeAgentId);

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
            publishStateChange(coordination);
            log.info("[CoordinationService] {} → FAILED (no available slots)", coordId);
            return coordId;
        }

        // Step 5: Store available slots and transition to PROPOSAL_GENERATED
        coordination.setAvailableSlots(matched);
        coordination.transition(CoordinationState.PROPOSAL_GENERATED,
                "Proposal generated with " + matched.size() + " available slots");
        persistence.save(coordination);
        publishStateChange(coordination);
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

        // Step 7: Request approval from invitee (B first)
        coordination.transition(CoordinationState.AWAITING_APPROVAL_B, "Awaiting invitee approval");
        persistence.save(coordination);
        log.info("[CoordinationService] {} → AWAITING_APPROVAL_B", coordinationId);

        var approvalId = agentApprovalPort.requestApproval(coordination.getInviteeAgentId(), proposal);
        log.info("[ApprovalService] Approval created id={} for invitee agent {}", approvalId,
                coordination.getInviteeAgentId());

        persistence.save(coordination);
        publishStateChange(coordination);
    }

    // ── Steps 7–9: Approval Handling ──

    @Override
    public void handleApproval(CoordinationId coordinationId, AgentId agentId, boolean approved) {
        log.info("[CoordinationService] Approval decision for {}: agent={} approved={}", coordinationId, agentId,
                approved);
        Coordination coordination = load(coordinationId);

        if (!approved) {
            coordination.transition(CoordinationState.REJECTED, "Rejected by agent " + agentId);
            persistence.save(coordination);
            publishStateChange(coordination);
            log.info("[CoordinationService] {} → REJECTED by agent {}", coordinationId, agentId);
            return;
        }

        CoordinationState currentState = coordination.getState();

        if (currentState == CoordinationState.AWAITING_APPROVAL_B) {
            // Invitee approved → now request requester approval
            coordination.transition(CoordinationState.AWAITING_APPROVAL_A, "Invitee approved, awaiting requester");
            persistence.save(coordination);
            log.info("[CoordinationService] {} → AWAITING_APPROVAL_A (invitee approved)", coordinationId);

            var approvalId = agentApprovalPort.requestApproval(
                    coordination.getRequesterAgentId(), coordination.getProposal());
            log.info("[ApprovalService] Approval created id={} for requester agent {}", approvalId,
                    coordination.getRequesterAgentId());

            persistence.save(coordination);
            publishStateChange(coordination);

        } else if (currentState == CoordinationState.AWAITING_APPROVAL_A) {
            // Requester approved → both approved → execute event creation saga
            coordination.transition(CoordinationState.APPROVED_BY_BOTH, "Both parties approved");
            persistence.save(coordination);
            log.info("[CoordinationService] {} → APPROVED_BY_BOTH", coordinationId);

            log.info("[CoordinationService] {} Executing event creation saga...", coordinationId);
            boolean success = eventCreationSaga.execute(coordination, agentEventExecutionPort);
            persistence.save(coordination);

            if (success) {
                log.info("[CoordinationService] {} → COMPLETED (both events created)", coordinationId);
            } else {
                log.error("[CoordinationService] {} → FAILED (event creation saga failed)", coordinationId);
            }

            publishStateChange(coordination);
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

        publishStateChange(coordination);
    }

    @Override
    public void terminate(CoordinationId coordinationId, String reason) {
        log.warn("[CoordinationService] TERMINATING {} | Reason: {}", coordinationId, reason);
        Coordination coordination = load(coordinationId);
        if (!coordination.isTerminal()) {
            coordination.transition(CoordinationState.FAILED, reason);
            persistence.save(coordination);
            publishStateChange(coordination);
        }
    }

    private Coordination load(CoordinationId id) {
        return persistence.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coordination not found: " + id));
    }

    private void publishStateChange(Coordination c) {
        List<CoordinationStateLogEntry> stateLog = c.getStateLog();
        if (stateLog.size() < 2)
            return;
        CoordinationStateLogEntry last = stateLog.get(stateLog.size() - 1);
        CoordinationStateLogEntry prev = stateLog.get(stateLog.size() - 2);
        eventPublisher.publish(CoordinationStateChanged.of(
                c.getCoordinationId(),
                prev.toState().name(),
                last.toState().name(),
                last.reason()));
    }
}
