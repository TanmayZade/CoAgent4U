package com.coagent4u.coordination.domain;

import java.time.Instant;
import java.util.Objects;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;

/** Confirms that a calendar event was successfully created for an agent. */
public record EventConfirmation(AgentId agentId, EventId eventId, Instant createdAt) {
    public EventConfirmation {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
