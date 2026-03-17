package com.coagent4u.app.listener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.agent.port.out.AgentPersistencePort;

import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Listens for coordination state changes and sends professional
 * Slack notifications when a coordination reaches a terminal state.
 */
@Component
public class CoordinationCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(CoordinationCompletedListener.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

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
    public void onCoordinationCompleted(com.coagent4u.common.events.CoordinationCompleted event) {
        handleCompleted(event.coordinationId());
        cleanupObsoleteMessages(event.coordinationId());
    }

    @Async
    @EventListener
    public void onCoordinationRejected(com.coagent4u.common.events.CoordinationRejected event) {
        handleRejected(event);
        cleanupObsoleteMessages(event.coordinationId());
    }

    @Async
    @EventListener
    public void onCoordinationFailed(com.coagent4u.common.events.CoordinationFailed event) {
        handleFailed(event.coordinationId(), event.reason());
        cleanupObsoleteMessages(event.coordinationId());
    }

    /**
     * Deletes obsolete invitation and notification messages once the coordination
     * is terminal.
     */
    private void cleanupObsoleteMessages(com.coagent4u.shared.CoordinationId coordId) {
        try {
            coordinationPersistence.findById(coordId).ifPresent(coordination -> {
                // Delete invitee invitation (original — may already be deleted by I1)
                String inviteeTs = coordination.getMetadata("invitee_invitation_ts");
                if (inviteeTs != null) {
                    deleteForAgent(coordination.getInviteeAgentId(), inviteeTs);
                }

                // Delete the interactive slot selection card (User B picks slot)
                String slotSelectionTs = coordination.getMetadata("slot_selection_ts");
                if (slotSelectionTs != null) {
                    deleteForAgent(coordination.getInviteeAgentId(), slotSelectionTs);
                }

                // Delete clean invitation header (reposted by I1)
                // String headerTs = coordination.getMetadata("invitee_header_ts");
                // if (headerTs != null) {
                // deleteForAgent(coordination.getInviteeAgentId(), headerTs);
                // }

                // Delete requester notification (may already be deleted by R1)
                String requesterTs = coordination.getMetadata("requester_notification_ts");
                if (requesterTs != null) {
                    deleteForAgent(coordination.getRequesterAgentId(), requesterTs);
                }

                // Delete intermediate "Waiting for approval" status card
                String selectedTs = coordination.getMetadata("selected_slot_ts");
                if (selectedTs != null) {
                    deleteForAgent(coordination.getInviteeAgentId(), selectedTs);
                }

                // Delete clean slot card (reposted by I2, without "Waiting for approval")
                String cleanSlotTs = coordination.getMetadata("clean_slot_ts");
                if (cleanSlotTs != null) {
                    deleteForAgent(coordination.getInviteeAgentId(), cleanSlotTs);
                }

                // Delete the intermediate "Meeting Approved" status card
                String finalStatusTs = coordination.getMetadata("final_status_ts");
                if (finalStatusTs != null) {
                    deleteForAgent(coordination.getRequesterAgentId(), finalStatusTs);
                }
            });
        } catch (Exception e) {
            log.warn("[CoordinationListener] Cleanup failed for {}: {}", coordId, e.getMessage());
        }
    }

    private void deleteForAgent(AgentId agentId, String ts) {
        try {
            var agent = agentPersistence.findById(agentId).orElse(null);
            if (agent == null)
                return;
            User user = userPersistence.findById(agent.getUserId()).orElse(null);
            if (user == null || user.getSlackIdentity() == null)
                return;

            notificationPort.deleteMessage(user.getSlackIdentity().slackUserId(), user.getSlackIdentity().workspaceId(), ts);
            log.info("[CoordinationListener] Deleted message ts={} for agent={}", ts, agentId);
        } catch (Exception e) {
            log.debug("[CoordinationListener] Could not delete message for agent={}: {}", agentId, e.getMessage());
        }
    }

    private void handleCompleted(com.coagent4u.shared.CoordinationId coordinationId) {
        try {
            Coordination coordination = coordinationPersistence.findById(coordinationId).orElse(null);
            if (coordination == null)
                return;

            String requesterMention = resolveSlackMention(coordination.getRequesterAgentId());
            String inviteeMention = resolveSlackMention(coordination.getInviteeAgentId());

            // Format meeting details
            TimeSlot slot = coordination.getSelectedSlot();
            String slotDetails = "";
            if (slot != null) {
                String dateStr = slot.start().atZone(IST).format(DATE_FMT);
                String startTime = slot.start().atZone(IST).format(TIME_FMT);
                String endTime = slot.end().atZone(IST).format(TIME_FMT);
                slotDetails = "\n\n📅 " + dateStr + "\n🕐 " + startTime + " – " + endTime;
            }

            String message = "✅ *Meeting Confirmed*\n\n"
                    + "*Participants:*\n"
                    + "• " + requesterMention + "\n"
                    + "• " + inviteeMention
                    + slotDetails
                    + "\n\nThe meeting has been added to your calendars.";

            notifyAgent(coordination.getRequesterAgentId(), message);
            notifyAgent(coordination.getInviteeAgentId(), message);

        } catch (Exception e) {
            log.warn("[CoordinationListener] Failed to send completion notification: {}", e.getMessage());
        }
    }

    private void handleRejected(com.coagent4u.common.events.CoordinationRejected event) {
        try {
            String reason = event.reason();
            String displayReason = reason;

            // Resolve Slack mention if reason follows machine-parseable format
            if (reason != null && reason.startsWith("REJECTED_BY_AGENT:")) {
                try {
                    String agentIdStr = reason.substring("REJECTED_BY_AGENT:".length());
                    java.util.UUID uuid = java.util.UUID.fromString(agentIdStr);
                    String mention = resolveSlackMention(new AgentId(uuid));
                    displayReason = "Rejected by " + mention;
                } catch (Exception e) {
                    log.debug("[CoordinationListener] Failed to parse rejection agent: {}", e.getMessage());
                }
            }

            String message = "🚫 *Meeting Rejected*\n\n"
                    + displayReason;

            // Notice we only notify the agent linked to this exact event (Symmetric Flow!)
            notifyAgent(event.agentId(), message);

        } catch (Exception e) {
            log.warn("[CoordinationListener] Failed to send rejection notification: {}", e.getMessage());
        }
    }

    private void handleFailed(com.coagent4u.shared.CoordinationId coordinationId, String reason) {
        try {
            Coordination coordination = coordinationPersistence.findById(coordinationId).orElse(null);
            if (coordination == null)
                return;

            String message = "❌ *Meeting Scheduling Failed*\n\n"
                    + reason;

            notifyAgent(coordination.getRequesterAgentId(), message);
            notifyAgent(coordination.getInviteeAgentId(), message);

        } catch (Exception e) {
            log.warn("[CoordinationListener] Failed to send failure notification: {}", e.getMessage());
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

    private String resolveSlackMention(AgentId agentId) {
        try {
            var agent = agentPersistence.findById(agentId).orElse(null);
            if (agent != null) {
                var user = userPersistence.findById(agent.getUserId()).orElse(null);
                if (user != null && user.getSlackIdentity() != null) {
                    return "<@" + user.getSlackIdentity().slackUserId().value() + ">";
                }
            }
        } catch (Exception ignored) {
        }
        return "a participant";
    }
}
