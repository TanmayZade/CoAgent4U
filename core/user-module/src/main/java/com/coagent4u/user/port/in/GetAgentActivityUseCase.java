package com.coagent4u.user.port.in;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.user.application.dto.AgentActivityEntry;

import java.time.Instant;

/**
 * Inbound port for agent activity queries.
 */
public interface GetAgentActivityUseCase {
    PaginatedResponse<AgentActivityEntry> getAgentActivity(
            String username, String eventTypeFilter, String levelFilter, Instant startDate, Instant endDate, int page, int size);
}
