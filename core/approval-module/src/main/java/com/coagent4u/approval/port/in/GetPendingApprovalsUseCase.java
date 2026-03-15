package com.coagent4u.approval.port.in;

import java.util.List;

import com.coagent4u.approval.application.dto.PendingApprovalDto;

/**
 * Inbound port for retrieving pending approvals for a user.
 */
public interface GetPendingApprovalsUseCase {
    List<PendingApprovalDto> getPendingApprovals(String username);
}
