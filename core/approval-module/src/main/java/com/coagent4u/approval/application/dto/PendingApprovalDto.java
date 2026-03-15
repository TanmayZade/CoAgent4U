package com.coagent4u.approval.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a pending approval card displayed on the dashboard.
 */
public record PendingApprovalDto(
        UUID approvalId,
        UUID coordinationId,
        String approvalType,
        Instant createdAt,
        Instant expiresAt
) {}
