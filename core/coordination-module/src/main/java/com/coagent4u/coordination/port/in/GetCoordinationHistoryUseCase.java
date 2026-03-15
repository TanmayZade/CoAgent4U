package com.coagent4u.coordination.port.in;

import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.shared.PaginatedResponse;

/**
 * Inbound port for coordination history queries.
 */
public interface GetCoordinationHistoryUseCase {
    PaginatedResponse<CoordinationSummary> getHistory(String username, int page, int size);
}
