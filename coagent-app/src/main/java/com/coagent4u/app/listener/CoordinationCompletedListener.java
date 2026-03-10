package com.coagent4u.app.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.common.events.CoordinationStateChanged;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Listens for coordination state changes and sends Slack notifications
 * when a coordination reaches a terminal state (COMPLETED, REJECTED, FAILED).
 */
@Component
public class CoordinationCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(CoordinationCompletedListener.class);

    private final CoordinationPersistencePort coordinationPersistence;
    private final AgentPersistencePort agentPersistence;
    private final UserPersistencePort userPersistence;
    private final NotificationPort notificationPort;

    public CoordinationCompletedListener(
            CoordinationPersistencePort coordinationPersistence,
            AgentPersistencePort agentPersistence,
            UserPersistencePort userPersistence,
            NotificationPort notificationPort) {
        this.coordinationPersistence = coordinationPersistence;
        this.agentPersistence = agentPersistence;
        this.userPersistence = userPersistence;
        this.notificationPort = notificationPort;
    }

    @Async
    @EventListener
    public void onCoordinationStateChanged(CoordinationStateChanged event) {
        String toState = event.toState();

        if ("COMPLETED".equals(toState)) {
            notifyBothUsers(event,
                    "✅ Meeting scheduled successfully! Calendar events have been created for both participants.");
        } else if ("REJECTED".equals(toState)) {
            notifyBothUsers(event, "🚫 Meeting coordination was rejected. " + event.reason());
        } else if ("FAILED".equals(toState)) {
            notifyBothUsers(event, "❌ Meeting coordination failed. " + event.reason());
        }
    }

    private void notifyBothUsers(CoordinationStateChanged event, String message) {
        try {
            var coordination = coordinationPersistence.findById(event.coordinationId()).orElse(null);
            if (coordination == null) {
                log.warn("[CoordinationListener] Coordination not found: {}", event.coordinationId());
                return;
            }

            notifyAgent(coordination.getRequesterAgentId(), message);
            notifyAgent(coordination.getInviteeAgentId(), message);

        } catch (Exception e) {
            log.warn("[CoordinationListener] Failed to send notification: {}", e.getMessage());
        }
    }

    private void notifyAgent(AgentId agentId, String message) {
        try {
            var agent = agentPersistence.findById(agentId).orElse(null);
            if (agent == null)
                return;

            User user = userPersistence.findById(agent.getUserId()).orElse(null);
            if (user == null || user.getSlackIdentity() == null)
                return;

            notificationPort.sendMessage(
                    user.getSlackIdentity().slackUserId(),
                    user.getSlackIdentity().workspaceId(),
                    message);

            log.info("[CoordinationListener] Notification sent to agent={}", agentId);
        } catch (Exception e) {
            log.warn("[CoordinationListener] Failed to notify agent={}: {}", agentId, e.getMessage());
        }
    }
}
