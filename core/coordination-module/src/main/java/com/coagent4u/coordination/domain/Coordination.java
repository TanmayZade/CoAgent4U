package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeSlot;

/**
 * Aggregate root for the Coordination bounded context.
 *
 * <p>
 * Represents the collaborative scheduling session between two agents.
 * State transitions are strictly validated by {@link CoordinationStateMachine}.
 * All state changes are logged via {@link CoordinationStateLogEntry}.
 */
public class Coordination {

    private static final CoordinationStateMachine STATE_MACHINE = new CoordinationStateMachine();

    private final CoordinationId coordinationId;
    private final AgentId requesterAgentId;
    private final AgentId inviteeAgentId;
    private CoordinationState state;
    private MeetingProposal proposal;
    private final List<CoordinationStateLogEntry> stateLog = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private List<TimeSlot> availableSlots = new ArrayList<>();
    private TimeSlot selectedSlot;

    public Coordination(CoordinationId coordinationId, AgentId requesterAgentId, AgentId inviteeAgentId) {
        this.coordinationId = Objects.requireNonNull(coordinationId);
        this.requesterAgentId = Objects.requireNonNull(requesterAgentId);
        this.inviteeAgentId = Objects.requireNonNull(inviteeAgentId);
        this.state = CoordinationState.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        // Log the initial state
        stateLog.add(CoordinationStateLogEntry.of(coordinationId, null, CoordinationState.INITIATED,
                "Coordination initiated"));
    }

    /**
     * Transitions to the next state after validation.
     *
     * @param toState the target state
     * @param reason  human-readable reason for the transition
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transition(CoordinationState toState, String reason) {
        STATE_MACHINE.validateTransition(this.state, toState);
        CoordinationState fromState = this.state;
        this.state = toState;
        this.updatedAt = Instant.now();
        if (toState.isTerminal()) {
            this.completedAt = this.updatedAt;
        }
        stateLog.add(CoordinationStateLogEntry.of(coordinationId, fromState, toState, reason));
    }

    public void setProposal(MeetingProposal proposal) {
        this.proposal = Objects.requireNonNull(proposal);
    }

    public boolean isTerminal() {
        return state.isTerminal();
    }

    // ── Slot selection ──

    /**
     * Stores the list of available (free) slots from slot matching.
     */
    public void setAvailableSlots(List<TimeSlot> slots) {
        this.availableSlots = new ArrayList<>(Objects.requireNonNull(slots));
    }

    /**
     * Records the user's selected slot from the available options.
     *
     * @throws IllegalArgumentException if the selected slot is not in
     *                                  availableSlots
     */
    public void selectSlot(TimeSlot slot) {
        Objects.requireNonNull(slot, "Selected slot must not be null");
        if (!availableSlots.contains(slot)) {
            throw new IllegalArgumentException("Selected slot is not in the available slots list");
        }
        this.selectedSlot = slot;
    }

    // Getters
    public CoordinationId getCoordinationId() {
        return coordinationId;
    }

    public AgentId getRequesterAgentId() {
        return requesterAgentId;
    }

    public AgentId getInviteeAgentId() {
        return inviteeAgentId;
    }

    public CoordinationState getState() {
        return state;
    }

    public MeetingProposal getProposal() {
        return proposal;
    }

    public List<CoordinationStateLogEntry> getStateLog() {
        return Collections.unmodifiableList(stateLog);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<TimeSlot> getAvailableSlots() {
        return Collections.unmodifiableList(availableSlots);
    }

    public TimeSlot getSelectedSlot() {
        return selectedSlot;
    }
}
