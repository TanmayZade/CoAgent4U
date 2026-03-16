package com.coagent4u.user.port.out;

import java.util.List;

import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AgentActivityEntry;

import java.time.Instant;

/**
 * Outbound port — read-only queries for agent activity data.
 * Implemented in the persistence module.
 */
public interface AgentActivityQueryPort {
    List<AgentActivityEntry> findByUserId(UserId userId, String eventTypeFilter, String levelFilter, Instant startDate, Instant endDate, int offset, int limit);

    long countByUserId(UserId userId, String eventTypeFilter, String levelFilter, Instant startDate, Instant endDate);

    List<AgentActivityEntry> findAllByUserId(UserId userId);
}
