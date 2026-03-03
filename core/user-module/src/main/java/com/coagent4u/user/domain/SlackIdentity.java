package com.coagent4u.user.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Value object representing a user's Slack identity.
 * Immutable representation of the link between a CoAgent4U user and a Slack
 * account.
 */
public record SlackIdentity(
        SlackUserId slackUserId,
        WorkspaceId workspaceId,
        Instant linkedAt) {
    public SlackIdentity {
        Objects.requireNonNull(slackUserId, "slackUserId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(linkedAt, "linkedAt must not be null");
    }

    public static SlackIdentity of(SlackUserId slackUserId, WorkspaceId workspaceId) {
        return new SlackIdentity(slackUserId, workspaceId, Instant.now());
    }
}
