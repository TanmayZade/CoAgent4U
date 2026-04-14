package com.coagent4u.coordination.domain.policy;

/**
 * Result of a policy evaluation.
 */
public record PolicyResult(Decision decision, String reason) {
    
    public enum Decision {
        ALLOW,
        REJECT,
        AUTO_SELECT_SLOT,
        REQUIRE_APPROVAL
    }

    public static PolicyResult allow(String reason) {
        return new PolicyResult(Decision.ALLOW, reason);
    }

    public static PolicyResult reject(String reason) {
        return new PolicyResult(Decision.REJECT, reason);
    }

    public static PolicyResult autoSelectSlot(String reason) {
        return new PolicyResult(Decision.AUTO_SELECT_SLOT, reason);
    }

    public static PolicyResult requireApproval(String reason) {
        return new PolicyResult(Decision.REQUIRE_APPROVAL, reason);
    }
}
