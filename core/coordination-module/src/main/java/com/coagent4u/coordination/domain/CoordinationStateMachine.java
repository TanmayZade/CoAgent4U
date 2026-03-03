package com.coagent4u.coordination.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain service that validates legal state transitions for the coordination
 * state machine.
 * This is the core invariant enforcer — illegal transitions throw
 * {@link IllegalStateException}.
 *
 * <p>
 * Allowed transitions:
 * 
 * <pre>
 * INITIATED            → CHECKING_AVAILABILITY_A, FAILED
 * CHECKING_AVAIL_A     → CHECKING_AVAILABILITY_B, FAILED
 * CHECKING_AVAIL_B     → MATCHING, FAILED
 * MATCHING             → PROPOSAL_GENERATED, FAILED
 * PROPOSAL_GENERATED   → AWAITING_APPROVAL_B, FAILED
 * AWAITING_APPROVAL_B  → AWAITING_APPROVAL_A, REJECTED, FAILED
 * AWAITING_APPROVAL_A  → APPROVED_BY_BOTH, REJECTED, FAILED
 * APPROVED_BY_BOTH     → CREATING_EVENT_A, FAILED
 * CREATING_EVENT_A     → CREATING_EVENT_B, FAILED
 * CREATING_EVENT_B     → COMPLETED, FAILED
 * COMPLETED            → (terminal)
 * REJECTED             → (terminal)
 * FAILED               → (terminal)
 * </pre>
 */
public class CoordinationStateMachine {

    private static final Map<CoordinationState, Set<CoordinationState>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(CoordinationState.INITIATED,
                    EnumSet.of(CoordinationState.CHECKING_AVAILABILITY_A, CoordinationState.FAILED)),
            Map.entry(CoordinationState.CHECKING_AVAILABILITY_A,
                    EnumSet.of(CoordinationState.CHECKING_AVAILABILITY_B, CoordinationState.FAILED)),
            Map.entry(CoordinationState.CHECKING_AVAILABILITY_B,
                    EnumSet.of(CoordinationState.MATCHING, CoordinationState.FAILED)),
            Map.entry(CoordinationState.MATCHING,
                    EnumSet.of(CoordinationState.PROPOSAL_GENERATED, CoordinationState.FAILED)),
            Map.entry(CoordinationState.PROPOSAL_GENERATED,
                    EnumSet.of(CoordinationState.AWAITING_APPROVAL_B, CoordinationState.FAILED)),
            Map.entry(CoordinationState.AWAITING_APPROVAL_B,
                    EnumSet.of(CoordinationState.AWAITING_APPROVAL_A,
                            CoordinationState.REJECTED, CoordinationState.FAILED)),
            Map.entry(CoordinationState.AWAITING_APPROVAL_A,
                    EnumSet.of(CoordinationState.APPROVED_BY_BOTH,
                            CoordinationState.REJECTED, CoordinationState.FAILED)),
            Map.entry(CoordinationState.APPROVED_BY_BOTH,
                    EnumSet.of(CoordinationState.CREATING_EVENT_A, CoordinationState.FAILED)),
            Map.entry(CoordinationState.CREATING_EVENT_A,
                    EnumSet.of(CoordinationState.CREATING_EVENT_B, CoordinationState.FAILED)),
            Map.entry(CoordinationState.CREATING_EVENT_B,
                    EnumSet.of(CoordinationState.COMPLETED, CoordinationState.FAILED)),
            Map.entry(CoordinationState.COMPLETED, EnumSet.noneOf(CoordinationState.class)),
            Map.entry(CoordinationState.REJECTED, EnumSet.noneOf(CoordinationState.class)),
            Map.entry(CoordinationState.FAILED, EnumSet.noneOf(CoordinationState.class)));

    /**
     * Validates that a transition from {@code current} to {@code next} is legal.
     *
     * @throws IllegalStateException if the transition is not allowed
     */
    public void validateTransition(CoordinationState current, CoordinationState next) {
        Set<CoordinationState> allowed = ALLOWED_TRANSITIONS.getOrDefault(current,
                EnumSet.noneOf(CoordinationState.class));
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                    "Illegal coordination state transition: " + current + " → " + next);
        }
    }

    public Set<CoordinationState> allowedTransitionsFrom(CoordinationState state) {
        return EnumSet.copyOf(ALLOWED_TRANSITIONS.getOrDefault(state, EnumSet.noneOf(CoordinationState.class)));
    }
}
