package com.coagent4u.approval.domain;

/**
 * Type of an approval request.
 * PERSONAL — user approving their own personal calendar action.
 * COLLABORATIVE — user approving a meeting proposal with another agent.
 */
public enum ApprovalType {
    PERSONAL,
    COLLABORATIVE
}
