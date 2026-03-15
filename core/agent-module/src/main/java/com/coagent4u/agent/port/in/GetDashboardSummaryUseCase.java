package com.coagent4u.agent.port.in;

import com.coagent4u.agent.application.dto.DashboardSummary;

/**
 * Inbound port for the main dashboard summary.
 */
public interface GetDashboardSummaryUseCase {
    DashboardSummary getSummary(String username);
}
