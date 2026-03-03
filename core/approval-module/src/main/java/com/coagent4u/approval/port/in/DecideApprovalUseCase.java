package com.coagent4u.approval.port.in;

import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

/**
 * Inbound port — records a user's approve/reject decision on an approval
 * request.
 */
public interface DecideApprovalUseCase {
    /**
     * @param approvalId the approval being decided
     * @param userId     the user making the decision (validated against approval
     *                   owner)
     * @param decision   must be APPROVED or REJECTED
     * @throws IllegalArgumentException if decision is not APPROVED or REJECTED
     * @throws IllegalStateException    if approval is already decided or expired
     * @throws SecurityException        if userId does not own this approval
     */
    void decide(ApprovalId approvalId, UserId userId, ApprovalStatus decision);
}
