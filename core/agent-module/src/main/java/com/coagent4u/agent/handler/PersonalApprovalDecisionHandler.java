package com.coagent4u.agent.handler;

import com.coagent4u.common.events.ApprovalDecisionMade;

/**
 * Handles {@link ApprovalDecisionMade} events for PERSONAL approvals.
 * The personal approval flow does not touch the coordination state machine.
 * The agent proceeds to create the calendar event directly on APPROVED.
 */
public class PersonalApprovalDecisionHandler {

    /**
     * @param event     the decision event
     * @param onApprove callback to execute the approved action (e.g. create event)
     */
    public void handle(ApprovalDecisionMade event, Runnable onApprove) {
        if (!"PERSONAL".equals(event.approvalType()))
            return;
        if ("APPROVED".equals(event.decision())) {
            onApprove.run();
        }
        // REJECTED — no further action needed; agent already notified user via Slack
    }
}
