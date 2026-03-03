package com.coagent4u.approval.port.in;

import com.coagent4u.approval.domain.ApprovalType;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.Duration;
import com.coagent4u.shared.UserId;

/**
 * Inbound port — creates a new approval request for a user.
 */
public interface CreateApprovalUseCase {
    /**
     * @param userId         the user who must decide
     * @param approvalType   PERSONAL or COLLABORATIVE
     * @param coordinationId the related coordination (null for PERSONAL)
     * @param timeout        how long before the approval expires
     * @return the generated ApprovalId
     */
    ApprovalId create(UserId userId, ApprovalType approvalType,
            CoordinationId coordinationId, Duration timeout);
}
