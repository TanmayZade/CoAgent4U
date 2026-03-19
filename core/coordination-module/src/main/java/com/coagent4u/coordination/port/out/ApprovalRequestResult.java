package com.coagent4u.coordination.port.out;

import com.coagent4u.shared.ApprovalId;

/**
 * Result of an approval request, including the Slack message timestamp.
 */
public record ApprovalRequestResult(ApprovalId approvalId, String messageTs) {
}
