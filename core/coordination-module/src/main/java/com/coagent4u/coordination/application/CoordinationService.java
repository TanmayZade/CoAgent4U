package com.coagent4u.coordination.application;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
    private final List<com.coagent4u.coordination.domain.policy.GovernancePolicy> governancePolicies;

    public CoordinationService(CoordinationPersistencePort persistence,
            AgentAvailabilityPort agentAvailabilityPort,
            AgentEventExecutionPort agentEventExecutionPort,
            AgentProfilePort agentProfilePort,
            AgentApprovalPort agentApprovalPort,
            DomainEventPublisher eventPublisher,
            List<com.coagent4u.coordination.domain.policy.GovernancePolicy> governancePolicies) {
        this.persistence = persistence;
        this.agentAvailabilityPort = agentAvailabilityPort;
        this.agentEventExecutionPort = agentEventExecutionPort;
        this.agentProfilePort = agentProfilePort;
        this.agentApprovalPort = agentApprovalPort;
        this.eventPublisher = eventPublisher;
        this.governancePolicies = governancePolicies;
    }

    // ── Step 1–5: Initiation through Slot Generation ──

    @Override
    public CoordinationId initiate(CoordinationId coordId, com.coagent4u.shared.CorrelationId correlationId, AgentId requesterAgentId, AgentId inviteeAgentId,
            TimeRange lookAheadRange, int durationMinutes,
            String title, String timezone) {

        Coordination coordination = new Coordination(coordId, requesterAgentId, inviteeAgentId, durationMinutes);
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

        // --- GOVERNANCE LAYER: Zero-Touch Auto-Scheduling ---
        if (evaluateGovernance(coordination, "Governance: Initial policy check after proposal generation.")) {
            log.info("[CoordinationService] {} Governance handled initiation, stopping further processing.", coordId);
            return coordId;
        }
        persistence.save(coordination);

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
        selectSlot(coordinationId, selectedSlot, "User selected a slot via web interface");
    }

    public void selectSlot(CoordinationId coordinationId, TimeSlot selectedSlot, String reason) {
        Coordination coordination = load(coordinationId);
        
        // Idempotency guard: only process if still in PROPOSAL_GENERATED
        if (coordination.getState() != CoordinationState.PROPOSAL_GENERATED) {
            log.info("[CoordinationService] {} Slot already selected (state={}), ignoring duplicate",
                    coordinationId, coordination.getState());
            return;
        }

        coordination.selectSlot(selectedSlot);
        log.info("[CoordinationService] {} selectedSlot={} reason={}", coordinationId, selectedSlot, reason);

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

        // Invitee's slot selection counts as their approval.
        coordination.transition(CoordinationState.AWAITING_APPROVAL_A, reason);
        persistence.save(coordination);

        // --- GOVERNANCE LAYER ---
        if (evaluateGovernance(coordination, "Governance: Policy check after slot selection.")) {
            log.info("[CoordinationService] {} Governance handled slot selection, stopping further processing.", coordinationId);
            return;
        }

        String correlationIdStr = coordination.getMetadata("correlationId");
        com.coagent4u.shared.CorrelationId correlationId = (correlationIdStr != null) 
                ? new com.coagent4u.shared.CorrelationId(java.util.UUID.fromString(correlationIdStr))
                : com.coagent4u.shared.CorrelationId.generate();

        eventPublisher.publish(com.coagent4u.common.events.SlotsReceived.of(coordination.getInviteeAgentId(), 
                agentProfilePort.getProfile(coordination.getInviteeAgentId()).userId(), 
                correlationId, coordinationId, 1));
        persistence.save(coordination);
    }

    private boolean evaluateGovernance(Coordination coordination, String context) {
        log.info("[CoordinationService] Evaluating governance ({}) for {}", context, coordination.getCoordinationId());
        
        for (com.coagent4u.coordination.domain.policy.GovernancePolicy policy : governancePolicies) {
            com.coagent4u.coordination.domain.policy.PolicyResult result = policy.evaluate(coordination);
            String policyName = policy.getClass().getSimpleName();
            log.info("[CoordinationService] Governance Policy Check: {} -> {}", 
                policyName, result.decision());

            if (result.decision() == com.coagent4u.coordination.domain.policy.PolicyResult.Decision.ALLOW) {
                log.info("[CoordinationService] Governance: Policy ALLOWED. Auto-approving session.");
                // Record the reason in the log
                String reason = "Governance Policy (" + policyName + ") auto-approved session: " + result.reason();
                
                // Simulate both parties approving
                this.handleApproval(coordination.getCoordinationId(), coordination.getRequesterAgentId(), true, reason);
                this.handleApproval(coordination.getCoordinationId(), coordination.getInviteeAgentId(), true, reason);
                return true;
            } else if (result.decision() == com.coagent4u.coordination.domain.policy.PolicyResult.Decision.REJECT) {
                log.warn("[CoordinationService] Governance: Policy REJECTED. Terminating session.");
                this.terminate(coordination.getCoordinationId(), "Governance Policy (" + policyName + ") REJECTED: " + result.reason());
                return true;
            } else if (result.decision() == com.coagent4u.coordination.domain.policy.PolicyResult.Decision.AUTO_SELECT_SLOT) {
                log.info("[CoordinationService] Governance: Policy AUTO_SELECT_SLOT. Automated agent picking time...");
                if (!coordination.getAvailableSlots().isEmpty()) {
                    com.coagent4u.shared.TimeSlot firstSlot = coordination.getAvailableSlots().get(0);
                    this.selectSlot(coordination.getCoordinationId(), firstSlot, "Governance Policy (" + policyName + ") auto-selected slot: " + result.reason());
                    return true;
                }
            }
        }

        // Default: Continue to manual approval ONLY if it's currently AWAITING_APPROVAL_A
        if (coordination.getState() == CoordinationState.AWAITING_APPROVAL_A) {
            log.info("[CoordinationService] Governance: Manual approval required for requester.");
            ApprovalRequestResult approvalResult = agentApprovalPort.requestApproval(coordination.getRequesterAgentId(), coordination.getProposal());
            if (approvalResult.messageTs() != null) {
                coordination.setMetadata("requester_approval_ts", approvalResult.messageTs());
            }
        }
        return false;
    }

    // ── Steps 7–9: Approval Handling ──

    @Override
    public void handleApproval(CoordinationId coordinationId, AgentId agentId, boolean approved) {
        handleApproval(coordinationId, agentId, approved, "Manual approval via UI/API");
    }

    public void handleApproval(CoordinationId coordinationId, AgentId approverAgentId, boolean approved, String reason) {
        Coordination coordination = load(coordinationId);
        log.info("[CoordinationService] handleApproval id={} agent={} approved={} reason={}", 
                coordinationId, approverAgentId, approved, reason);

        String correlationIdStr = coordination.getMetadata("correlationId");
        com.coagent4u.shared.CorrelationId correlationId = (correlationIdStr != null) 
                ? new com.coagent4u.shared.CorrelationId(java.util.UUID.fromString(correlationIdStr))
                : com.coagent4u.shared.CorrelationId.generate();

        if (!approved) {
            String rejectReason = "REJECTED_BY_AGENT:" + approverAgentId.value();
            coordination.transition(CoordinationState.REJECTED, "Manual rejection: " + reason);
            persistence.save(coordination);
            log.info("[CoordinationService] {} → REJECTED by agent {}", coordinationId, approverAgentId);

            com.coagent4u.shared.UserId rUserId = agentProfilePort.getProfile(coordination.getRequesterAgentId()).userId();
            com.coagent4u.shared.UserId iUserId = agentProfilePort.getProfile(coordination.getInviteeAgentId()).userId();
            
            eventPublisher.publish(com.coagent4u.common.events.CoordinationRejected.of(
                    coordination.getRequesterAgentId(), rUserId, correlationId, coordinationId, rejectReason));
            eventPublisher.publish(com.coagent4u.common.events.CoordinationRejected.of(
                    coordination.getInviteeAgentId(), iUserId, correlationId, coordinationId, rejectReason));
            return;
        }

        CoordinationState currentState = coordination.getState();
        boolean isRequester = approverAgentId.equals(coordination.getRequesterAgentId());

        if (currentState == CoordinationState.AWAITING_APPROVAL_A && isRequester) {
            coordination.transition(CoordinationState.APPROVED_BY_BOTH, reason);
            persistence.save(coordination);
            log.info("[CoordinationService] {} → APPROVED_BY_BOTH", coordinationId);

            log.info("[CoordinationService] {} Executing event creation saga...", coordinationId);
            com.coagent4u.coordination.domain.EventCreationSaga.SagaResult sagaResult = eventCreationSaga.execute(coordination, agentEventExecutionPort);
            persistence.save(coordination);

            if (sagaResult.success()) {
                log.info("[CoordinationService] {} → COMPLETED", coordinationId);
                persistence.save(coordination);

                com.coagent4u.shared.UserId rUserId = agentProfilePort.getProfile(coordination.getRequesterAgentId()).userId();
                com.coagent4u.shared.UserId iUserId = agentProfilePort.getProfile(coordination.getInviteeAgentId()).userId();

                eventPublisher.publish(com.coagent4u.common.events.CalendarEventCreated.of(
                        coordination.getRequesterAgentId(), rUserId, correlationId, coordinationId, sagaResult.eventIdA()));
                eventPublisher.publish(com.coagent4u.common.events.CalendarEventCreated.of(
                        coordination.getInviteeAgentId(), iUserId, correlationId, coordinationId, sagaResult.eventIdB()));
                
                eventPublisher.publish(com.coagent4u.common.events.CoordinationCompleted.of(
                        coordinationId, sagaResult.eventIdA(), sagaResult.eventIdB()));
            } else {
                log.error("[CoordinationService] {} → FAILED (saga failed)", coordinationId);
                persistence.save(coordination);
                eventPublisher.publish(com.coagent4u.common.events.CoordinationFailed.of(coordinationId, "Event creation saga failed"));
            }
        } else {
            log.info("[CoordinationService] {} - Approval recorded but not yet ready for final state", coordinationId);
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

    @Override
    public CoordinationState getState(CoordinationId coordinationId) {
        return load(coordinationId).getState();
    }

    private Coordination load(CoordinationId id) {
        return persistence.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coordination not found: " + id));
    }

}
