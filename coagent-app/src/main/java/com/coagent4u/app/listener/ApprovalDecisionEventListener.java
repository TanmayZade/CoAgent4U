package com.coagent4u.app.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.agent.application.AgentCommandService;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.common.events.ApprovalDecisionMade;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

/**
 * Listens for {@link ApprovalDecisionMade} domain events and routes them:
 * - PERSONAL approvals → AgentCommandService (calendar event creation)
 * - COLLABORATIVE approvals → CoordinationProtocolPort (coordination state
 * machine)
 */
@Component
public class ApprovalDecisionEventListener {

    private static final Logger log = LoggerFactory.getLogger(ApprovalDecisionEventListener.class);

    private final AgentCommandService agentCommandService;
    private final CoordinationProtocolPort coordinationProtocol;
    private final AgentPersistencePort agentPersistencePort;

    public ApprovalDecisionEventListener(
            AgentCommandService agentCommandService,
            CoordinationProtocolPort coordinationProtocol,
            AgentPersistencePort agentPersistencePort) {
        this.agentCommandService = agentCommandService;
        this.coordinationProtocol = coordinationProtocol;
        this.agentPersistencePort = agentPersistencePort;
    }

    @Async
    @EventListener
    public void onApprovalDecision(ApprovalDecisionMade event) {
        log.info("[EventListener] ApprovalDecisionMade: approval={} decision={} type={} coordId={}",
                event.approvalId(), event.decision(), event.approvalType(), event.coordinationId());

        if ("PERSONAL".equals(event.approvalType())) {
            handlePersonalApproval(event);
        } else if ("COLLABORATIVE".equals(event.approvalType())) {
            handleCollaborativeApproval(event);
        } else {
            log.warn("[EventListener] Unknown approval type: {}", event.approvalType());
        }
    }

    private void handlePersonalApproval(ApprovalDecisionMade event) {
        try {
            agentCommandService.onPersonalApprovalDecided(
                    event.approvalId(),
                    event.decision(),
                    event.userId().value().toString());
        } catch (Exception e) {
            log.warn("[EventListener] Failed to process personal approval: {}", e.getMessage());
        }
    }

    private void handleCollaborativeApproval(ApprovalDecisionMade event) {
        try {
            if (event.coordinationId() == null) {
                log.warn("[EventListener] COLLABORATIVE approval {} has no coordinationId — cannot route",
                        event.approvalId());
                return;
            }

            // Resolve userId → agentId
            UserId userId = event.userId();
            AgentId agentId = agentPersistencePort.findByUserId(userId)
                    .map(agent -> agent.getAgentId())
                    .orElse(null);

            if (agentId == null) {
                log.warn("[EventListener] No agent found for userId={}", userId);
                return;
            }

            boolean approved = "APPROVED".equals(event.decision());

            log.info("[EventListener] Routing COLLABORATIVE approval to coordination: coord={} agent={} approved={}",
                    event.coordinationId(), agentId, approved);

            coordinationProtocol.handleApproval(event.coordinationId(), agentId, approved);

        } catch (Exception e) {
            log.warn("[EventListener] Failed to process collaborative approval: {}", e.getMessage());
        }
    }
}
