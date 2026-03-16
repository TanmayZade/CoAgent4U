package com.coagent4u.user.port.in;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.user.application.dto.AgentActivityEntry;

/**
 * Inbound port for agent activity queries.
 */
public interface GetAgentActivityUseCase {
    PaginatedResponse<AgentActivityEntry> getAgentActivity(
            String username, String eventTypeFilter, int page, int size);
}
