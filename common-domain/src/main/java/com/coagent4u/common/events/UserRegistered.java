package com.coagent4u.common.events;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

/** Published when a new user successfully registers in CoAgent4U. */
public record UserRegistered(
        UserId userId,
        String username,
        Email email,
        SlackUserId slackUserId,
        WorkspaceId workspaceId,
        Instant occurredAt) implements DomainEvent {
    public UserRegistered {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(slackUserId, "slackUserId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static UserRegistered of(UserId userId, String username, Email email,
            SlackUserId slackUserId, WorkspaceId workspaceId) {
        return new UserRegistered(userId, username, email, slackUserId, workspaceId, Instant.now());
    }
}
