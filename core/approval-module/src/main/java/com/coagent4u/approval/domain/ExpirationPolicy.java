package com.coagent4u.approval.domain;

import java.time.Instant;

/**
 * Domain service encapsulating the expiration policy for approval requests.
 * Default timeout: 12 hours (per PRD §4.3).
 */
public class ExpirationPolicy {

    public static final long DEFAULT_TIMEOUT_HOURS = 12;

    private ExpirationPolicy() {
    }

    /**
     * @return true if the approval has passed its expiration time
     */
    public static boolean isExpired(Approval approval, Instant now) {
        return approval.getStatus() == ApprovalStatus.PENDING
                && now.isAfter(approval.getExpiresAt());
    }

    /**
     * Calculates the expiry instant from a creation time and timeout hours.
     */
    public static Instant calculateExpiresAt(Instant createdAt, long timeoutHours) {
        return createdAt.plusSeconds(timeoutHours * 3600);
    }
}
