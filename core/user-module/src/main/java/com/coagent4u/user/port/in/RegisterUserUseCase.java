package com.coagent4u.user.port.in;

import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Inbound port — registers a new user from a Slack onboarding event.
 */
public interface RegisterUserUseCase {
    /**
     * @param userId      pre-generated or provided user ID
     * @param username    Slack display name (validated: lowercase alphanum +
     *                    underscore)
     * @param email       optional email address (may be null for Slack-only users)
     * @param slackUserId Slack platform user ID
     * @param workspaceId Slack workspace / team ID
     * @throws IllegalArgumentException if username format is invalid or user
     *                                  already exists
     */
    void register(UserId userId, String username, Email email,
            SlackUserId slackUserId, WorkspaceId workspaceId,
            String workspaceName, String workspaceDomain, String slackEmail, String displayName, String avatarUrl);
}
