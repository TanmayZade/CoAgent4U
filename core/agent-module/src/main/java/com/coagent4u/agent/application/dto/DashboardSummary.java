package com.coagent4u.agent.application.dto;

import java.time.Instant;
import java.util.List;

import com.coagent4u.coordination.application.dto.CoordinationSummary;

/**
 * Summary data for the main dashboard view.
 */
public record DashboardSummary(
        AgentStatusDto agentStatus,
        int pendingApprovalsCount,
        List<CoordinationSummary> recentCoordinations,
        List<ActivityPointDto> activitySummary
) {
    public record AgentStatusDto(
            String status, // ACTIVE, INACTIVE
            boolean slackConnected,
            boolean googleCalendarConnected,
            Instant lastActivityAt
    ) {}

    public record ActivityPointDto(
            String day,
            int completed,
            int rejected,
            int failed
    ) {}
}
