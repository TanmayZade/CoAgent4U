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
         * @return the Slack message timestamp (ts)
         */
        String sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String message);

        /**
         * Sends an interactive approval request with [Approve] and [Reject] buttons.
         *
         * @param slackUserId  recipient's Slack user ID
         * @param workspaceId  the Slack workspace
         * @param proposalText human-readable proposal details
         * @param approvalId   the approval ID to embed in button actions
         * @return the Slack message timestamp (ts)
         */
        String sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
                        String proposalText, String approvalId, String coordinationId);

        /**
         * Sends a slot selection card with multiple time-slot buttons.
         *
         * @param slackUserId      recipient's Slack user ID
         * @param workspaceId      the Slack workspace
         * @param coordinationId   the coordination session ID
         * @param slots            available time slots to choose from
         * @param requesterMention the requester's display mention (e.g.
         *                         {@code <@U12345>})
         * @return the Slack message timestamp (ts)
         */
        String sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
                        String coordinationId, java.util.List<com.coagent4u.shared.TimeSlot> slots,
                        String requesterMention);

        /**
         * Sends a read-only slot preview card to the requester.
         *
         * @param slackUserId      recipient's Slack user ID (requester)
         * @param workspaceId      the Slack workspace
         * @param slots            the slots that were proposed
         * @param inviteeMention   the invitee's display mention
         * @return the Slack message timestamp (ts)
         */
        String sendSlotPreview(SlackUserId slackUserId, WorkspaceId workspaceId,
                        java.util.List<com.coagent4u.shared.TimeSlot> slots,
                        String inviteeMention);

        /**
         * Sends a formatted status card with a colored sidebar.
         *
         * @param slackUserId recipient's Slack user ID
         * @param workspaceId the Slack workspace
         * @param statusText  the markdown text to display
         * @param color       hex color for the sidebar (e.g. "#3AA3E3")
         * @return the Slack message timestamp (ts)
         */
        String sendStatusCard(SlackUserId slackUserId, WorkspaceId workspaceId, String statusText, String color);

        /**
         * Deletes an existing message.
         *
         * @param slackUserId the user ID (channel)
         * @param ts          the message timestamp
         * @return true if deleted
         */
        boolean deleteMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String ts);
}
