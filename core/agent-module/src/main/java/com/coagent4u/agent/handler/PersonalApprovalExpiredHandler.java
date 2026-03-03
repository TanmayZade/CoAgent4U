package com.coagent4u.agent.handler;

import com.coagent4u.common.events.ApprovalExpired;

/**
 * Handles {@link ApprovalExpired} events for PERSONAL approvals.
 * Notifies the user that their pending action has timed out.
 */
public class PersonalApprovalExpiredHandler {

    /**
     * @param event     the expired event
     * @param onExpired callback to notify the user (e.g. send Slack DM)
     */
    public void handle(ApprovalExpired event, Runnable onExpired) {
        if (!"PERSONAL".equals(event.approvalType()))
            return;
        onExpired.run();
    }
}
