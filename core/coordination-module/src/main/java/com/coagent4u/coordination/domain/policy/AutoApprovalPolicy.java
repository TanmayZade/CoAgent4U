package com.coagent4u.coordination.domain.policy;

import com.coagent4u.coordination.domain.Coordination;

/**
 * A concrete policy that auto-approves meetings based on duration.
 */
public class AutoApprovalPolicy implements GovernancePolicy {

    private final int maxMinutesForAutoApproval;

    public AutoApprovalPolicy(int maxMinutesForAutoApproval) {
        this.maxMinutesForAutoApproval = maxMinutesForAutoApproval;
    }

    @Override
    public PolicyResult evaluate(Coordination coordination) {
        // --- STAGE 1: Auto-Selection (during PROPOSAL_GENERATED) ---
        if (coordination.getState() == com.coagent4u.coordination.domain.CoordinationState.PROPOSAL_GENERATED) {
            if (coordination.getDurationMinutes() <= maxMinutesForAutoApproval && !coordination.getAvailableSlots().isEmpty()) {
                return PolicyResult.autoSelectSlot("Governance: Auto-selecting slot for coordination under " + maxMinutesForAutoApproval + " minutes.");
            }
        }

        // --- STAGE 2: Auto-Approval (during AWAITING_APPROVAL_A) ---
        if (coordination.getProposal() != null && coordination.getProposal().durationMinutes() <= maxMinutesForAutoApproval) {
            return PolicyResult.allow("Governance: Auto-approving session under " + maxMinutesForAutoApproval + " minutes.");
        }

        return PolicyResult.requireApproval("Governance: Manual approval required for sessions over " + maxMinutesForAutoApproval + " minutes.");
    }
}
