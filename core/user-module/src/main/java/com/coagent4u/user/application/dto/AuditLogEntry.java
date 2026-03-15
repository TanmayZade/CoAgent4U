package com.coagent4u.user.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single audit log entry displayed on the dashboard.
 */
public record AuditLogEntry(
        UUID logId,
        String eventType,
        String payload,
        String correlationId,
        Instant occurredAt
) {}
