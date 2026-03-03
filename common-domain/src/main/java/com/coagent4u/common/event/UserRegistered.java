package com.coagent4u.common.event;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.shared.Email;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new user registers via Slack.
 * Consumed by agent-module to provision a personal agent.
 */
public record UserRegistered(
        UUID eventId,
        Instant occurredAt,
        UserId userId,
        SlackUserId slackUserId,
        WorkspaceId workspaceId,
        Email email
) implements DomainEvent {

    public UserRegistered(UserId userId, SlackUserId slackUserId, WorkspaceId workspaceId, Email email) {
        this(UUID.randomUUID(), Instant.now(), userId, slackUserId, workspaceId, email);
    }
}
