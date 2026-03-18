package com.coagent4u.coordination.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * High-level coordination summary for list views.
 */
public record CoordinationSummary(
        UUID coordinationId,
        String withUsername,
        String withDisplayName,
        String withAvatarUrl,
        String role,
        String state,
        Instant createdAt,
        String meetingTitle,
        Instant meetingTime
) {}
