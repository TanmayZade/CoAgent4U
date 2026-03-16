package com.coagent4u.user.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single agent activity entry displayed on the dashboard.
 */
public record AgentActivityEntry(
        UUID logId,
        UUID correlationId,
        UUID coordinationId,
        String eventType,
        String description,
        String level,
        Instant occurredAt
) {}
