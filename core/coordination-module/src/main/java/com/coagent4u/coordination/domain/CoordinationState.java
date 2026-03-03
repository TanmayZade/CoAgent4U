package com.coagent4u.coordination.domain;

/**
 * The 14 states of a Coordination lifecycle.
 *
 * <p>
 * Terminal states: COMPLETED, REJECTED, FAILED.
 * All other states are intermediate.
 */
public enum CoordinationState {
    // Initiated by requester agent
    INITIATED,

    // Gathering availability
    CHECKING_AVAILABILITY_A,
    CHECKING_AVAILABILITY_B,

    // Matching and proposing
    MATCHING,
    PROPOSAL_GENERATED,

    // Awaiting approval
    AWAITING_APPROVAL_B,
    AWAITING_APPROVAL_A,

    // Both approved — creating calendar events
    APPROVED_BY_BOTH,
    CREATING_EVENT_A,
    CREATING_EVENT_B,

    // Terminal states
    COMPLETED,
    REJECTED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == FAILED;
    }
}
