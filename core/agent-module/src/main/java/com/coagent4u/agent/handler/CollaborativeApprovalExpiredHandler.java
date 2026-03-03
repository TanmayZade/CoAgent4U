package com.coagent4u.agent.handler;

import com.coagent4u.common.events.ApprovalExpired;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.CoordinationId;

/**
 * Handles {@link ApprovalExpired} events for COLLABORATIVE approvals.
 * Terminates the coordination with a FAILED state.
 */
public class CollaborativeApprovalExpiredHandler {

    private final CoordinationProtocolPort coordinationProtocol;

    public CollaborativeApprovalExpiredHandler(CoordinationProtocolPort coordinationProtocol) {
        this.coordinationProtocol = coordinationProtocol;
    }

    public void handle(ApprovalExpired event, CoordinationId coordinationId) {
        if (!"COLLABORATIVE".equals(event.approvalType()))
            return;
        coordinationProtocol.terminate(coordinationId,
                "Approval expired for user " + event.userId() + " at " + event.expiredAt());
    }
}
