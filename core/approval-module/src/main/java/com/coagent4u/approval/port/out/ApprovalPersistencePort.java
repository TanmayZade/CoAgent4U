package com.coagent4u.approval.port.out;

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
}
