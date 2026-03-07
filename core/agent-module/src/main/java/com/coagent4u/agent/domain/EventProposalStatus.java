package com.coagent4u.agent.domain;

/**
 * State machine states for an event proposal.
 *
 * <pre>
 * INITIATED → PROPOSAL_GENERATED → AWAITING_USER_APPROVAL
 *                                       ↓             ↓
 *                                   APPROVED      REJECTED
 *                                       ↓
 *                                 EVENT_CREATED
 *                                       ↓
 *                                   COMPLETED
 *                                   (or FAILED)
 * </pre>
 */
public enum EventProposalStatus {
    INITIATED,
    PROPOSAL_GENERATED,
    AWAITING_USER_APPROVAL,
    APPROVED,
    REJECTED,
    EVENT_CREATED,
    COMPLETED,
    FAILED;

    /**
     * Validates that the transition from this status to the target is legal.
     *
     * @throws IllegalStateException if the transition is not allowed
     */
    public void validateTransitionTo(EventProposalStatus target) {
        boolean valid = switch (this) {
            case INITIATED -> target == PROPOSAL_GENERATED;
            case PROPOSAL_GENERATED -> target == AWAITING_USER_APPROVAL;
            case AWAITING_USER_APPROVAL -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == EVENT_CREATED || target == FAILED;
            case EVENT_CREATED -> target == COMPLETED;
            case REJECTED, COMPLETED, FAILED -> false; // terminal states
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Invalid proposal state transition: " + this + " → " + target);
        }
    }
}
