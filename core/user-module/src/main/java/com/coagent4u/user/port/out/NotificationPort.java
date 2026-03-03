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
}
