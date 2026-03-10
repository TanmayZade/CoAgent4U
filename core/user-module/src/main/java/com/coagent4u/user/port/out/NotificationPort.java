package com.coagent4u.user.port.out;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Outbound port — sends notifications to users via Slack.
 * Implemented in the messaging-module (SlackNotificationAdapter).
 */
public interface NotificationPort {
        /**
         * Sends a plain-text message to a user's Slack DM.
         *
         * @param slackUserId recipient's Slack user ID
         * @param workspaceId the Slack workspace
         * @param message     the message text to send
         */
        void sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String message);

        /**
         * Sends an interactive approval request with [Approve] and [Reject] buttons.
         *
         * @param slackUserId  recipient's Slack user ID
         * @param workspaceId  the Slack workspace
         * @param proposalText human-readable proposal details
         * @param approvalId   the approval ID to embed in button actions
         */
        void sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
                        String proposalText, String approvalId);

        /**
         * Sends a slot selection card with multiple time-slot buttons.
         *
         * @param slackUserId      recipient's Slack user ID
         * @param workspaceId      the Slack workspace
         * @param coordinationId   the coordination session ID
         * @param slots            available time slots to choose from
         * @param requesterMention the requester's display mention (e.g.
         *                         {@code <@U12345>})
         */
        void sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
                        String coordinationId, java.util.List<com.coagent4u.shared.TimeSlot> slots,
                        String requesterMention);
}
