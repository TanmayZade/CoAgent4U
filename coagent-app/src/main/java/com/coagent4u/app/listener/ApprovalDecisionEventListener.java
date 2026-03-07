package com.coagent4u.app.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.agent.application.AgentCommandService;
import com.coagent4u.common.events.ApprovalDecisionMade;

/**
 * Listens for {@link ApprovalDecisionMade} domain events and routes
 * PERSONAL approval decisions to the Agent command service for
 * calendar event creation.
 */
@Component
public class ApprovalDecisionEventListener {

    private static final Logger log = LoggerFactory.getLogger(ApprovalDecisionEventListener.class);

    private final AgentCommandService agentCommandService;

    public ApprovalDecisionEventListener(AgentCommandService agentCommandService) {
        this.agentCommandService = agentCommandService;
    }

    @Async
    @EventListener
    public void onApprovalDecision(ApprovalDecisionMade event) {
        log.info("[EventListener] ApprovalDecisionMade: approval={} decision={} type={}",
                event.approvalId(), event.decision(), event.approvalType());

        if (!"PERSONAL".equals(event.approvalType())) {
            log.info("[EventListener] Skipping non-PERSONAL approval: {}", event.approvalType());
            return;
        }

        try {
            agentCommandService.onPersonalApprovalDecided(
                    event.approvalId(),
                    event.decision(),
                    event.userId().value().toString());
        } catch (Exception e) {
            log.error("[EventListener] Failed to process approval decision: {}", e.getMessage(), e);
        }
    }
}
