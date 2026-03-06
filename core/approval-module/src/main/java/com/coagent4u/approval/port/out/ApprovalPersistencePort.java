package com.coagent4u.approval.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.coagent4u.approval.domain.Approval;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.UserId;

/**
 * Outbound port — persistence operations for the Approval aggregate.
 * Implemented in the persistence module (ApprovalPersistenceAdapter).
 */
public interface ApprovalPersistencePort {
    Approval save(Approval approval);

    Optional<Approval> findById(ApprovalId approvalId);

    List<Approval> findPendingByUser(UserId userId);

    /**
     * Finds all PENDING approvals whose expiration time is before {@code now}.
     * Used by the scheduled expiration job.
     */
    List<Approval> findExpiredPending(Instant now);
}
