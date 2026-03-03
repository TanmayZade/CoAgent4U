package com.coagent4u.agent.handler;

import com.coagent4u.common.events.ApprovalDecisionMade;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.CoordinationId;

/**
 * Handles {@link ApprovalDecisionMade} events for COLLABORATIVE approvals.
 * Routes the decision to advance or terminate the coordination state machine.
 */
public class CollaborativeApprovalDecisionHandler {

    private final CoordinationProtocolPort coordinationProtocol;

    public CollaborativeApprovalDecisionHandler(CoordinationProtocolPort coordinationProtocol) {
        this.coordinationProtocol = coordinationProtocol;
    }

    public void handle(ApprovalDecisionMade event, CoordinationId coordinationId, boolean isInvitee) {
        if (!"COLLABORATIVE".equals(event.approvalType()))
            return;

        if ("APPROVED".equals(event.decision())) {
            CoordinationState nextState = isInvitee
                    ? CoordinationState.AWAITING_APPROVAL_A
                    : CoordinationState.APPROVED_BY_BOTH;
            coordinationProtocol.advance(coordinationId, nextState,
                    "User " + event.userId() + " approved the meeting proposal");
        } else {
            coordinationProtocol.terminate(coordinationId,
                    "User " + event.userId() + " rejected the meeting proposal");
        }
    }
}
