package com.coagent4u.user.application;

import java.util.List;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AgentActivityEntry;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.GetAgentActivityUseCase;
import com.coagent4u.user.port.out.AgentActivityQueryPort;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Application service for agent activity queries.
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class AgentActivityQueryService implements GetAgentActivityUseCase {

    private final UserQueryPort userQuery;
    private final AgentActivityQueryPort agentActivityQuery;

    public AgentActivityQueryService(UserQueryPort userQuery, AgentActivityQueryPort agentActivityQuery) {
        this.userQuery = userQuery;
        this.agentActivityQuery = agentActivityQuery;
    }

    @Override
    public PaginatedResponse<AgentActivityEntry> getAgentActivity(
            String username, String eventTypeFilter, int page, int size) {
        User user = userQuery.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

        UserId userId = user.getUserId();
        int offset = page * size;

        List<AgentActivityEntry> entries = agentActivityQuery.findByUserId(userId, eventTypeFilter, offset, size);
        long total = agentActivityQuery.countByUserId(userId, eventTypeFilter);

        return new PaginatedResponse<>(entries, page, size, total,
                (int) Math.ceil((double) total / size));
    }
}
