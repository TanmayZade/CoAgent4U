package com.coagent4u.coordination.application;

import java.util.List;
import java.util.NoSuchElementException;

import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.common.events.CoordinationStateChanged;
import com.coagent4u.coordination.domain.AvailabilityBlock;
import com.coagent4u.coordination.domain.AvailabilityMatcher;
import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.domain.CoordinationStateLogEntry;
import com.coagent4u.coordination.domain.EventCreationSaga;
import com.coagent4u.coordination.domain.MeetingProposal;
import com.coagent4u.coordination.domain.ProposalGenerator;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.coordination.port.out.AgentApprovalPort;
import com.coagent4u.coordination.port.out.AgentAvailabilityPort;
import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.coordination.port.out.AgentProfilePort;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeRange;

/**
 * Application service for the Coordination bounded context.
 * Implements {@link CoordinationProtocolPort}.
 *
 * <p>
 * Orchestrates the full coordination lifecycle: initiation → availability →
 * proposal → approval → event creation saga → completion.
 *
 * <p>
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class CoordinationService implements CoordinationProtocolPort {

    private final CoordinationPersistencePort persistence;
    private final AgentAvailabilityPort agentAvailabilityPort;
    private final AgentEventExecutionPort agentEventExecutionPort;
    private final AgentProfilePort agentProfilePort;
    private final AgentApprovalPort agentApprovalPort;
    private final DomainEventPublisher eventPublisher;

    private final AvailabilityMatcher availabilityMatcher = new AvailabilityMatcher();
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

    @Override
    public CoordinationId initiate(AgentId requesterAgentId, AgentId inviteeAgentId,
            TimeRange lookAheadRange, int durationMinutes,
            String title, String timezone) {
        CoordinationId coordId = CoordinationId.generate();
        Coordination coordination = new Coordination(coordId, requesterAgentId, inviteeAgentId);
        persistence.save(coordination);

        // Begin availability phase
        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_A, "Checking requester availability");
        List<AvailabilityBlock> availA = agentAvailabilityPort.getAvailability(requesterAgentId, lookAheadRange);
        persistence.save(coordination);

        coordination.transition(CoordinationState.CHECKING_AVAILABILITY_B, "Checking invitee availability");
        List<AvailabilityBlock> availB = agentAvailabilityPort.getAvailability(inviteeAgentId, lookAheadRange);
        persistence.save(coordination);

        coordination.transition(CoordinationState.MATCHING, "Matching availability");
        var matched = availabilityMatcher.findOverlap(availA, availB,
                com.coagent4u.shared.Duration.of(durationMinutes));

        if (matched.isEmpty()) {
            coordination.transition(CoordinationState.FAILED, "No overlapping availability found");
            persistence.save(coordination);
            publishStateChange(coordination);
            return coordId;
        }

        AgentProfilePort.AgentProfile requesterProfile = agentProfilePort.getProfile(requesterAgentId);
        MeetingProposal proposal = proposalGenerator.generate(
                coordId.toString(), requesterAgentId, inviteeAgentId,
                matched.get(), durationMinutes, title, timezone);
        coordination.setProposal(proposal);

        coordination.transition(CoordinationState.PROPOSAL_GENERATED, "Proposal generated");
        persistence.save(coordination);

        // Request approval from invitee first (B), then requester (A)
        coordination.transition(CoordinationState.AWAITING_APPROVAL_B, "Awaiting invitee approval");
        agentApprovalPort.requestApproval(inviteeAgentId, proposal);
        persistence.save(coordination);

        publishStateChange(coordination);
        return coordId;
    }

    @Override
    public void advance(CoordinationId coordinationId, CoordinationState toState, String reason) {
        Coordination coordination = load(coordinationId);
        coordination.transition(toState, reason);
        persistence.save(coordination);

        // If both approved — run the event creation saga
        if (toState == CoordinationState.APPROVED_BY_BOTH) {
            eventCreationSaga.execute(coordination, agentEventExecutionPort);
            persistence.save(coordination);
        }

        publishStateChange(coordination);
    }

    @Override
    public void terminate(CoordinationId coordinationId, String reason) {
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
        List<CoordinationStateLogEntry> log = c.getStateLog();
        if (log.size() < 2)
            return;
        CoordinationStateLogEntry last = log.get(log.size() - 1);
        CoordinationStateLogEntry prev = log.get(log.size() - 2);
        eventPublisher.publish(CoordinationStateChanged.of(
                c.getCoordinationId(),
                prev.toState().name(),
                last.toState().name(),
                last.reason()));
    }
}
