package com.coagent4u.user.port.in;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.user.application.dto.AuditLogEntry;

/**
 * Inbound port for audit log queries.
 */
public interface GetAuditLogUseCase {
    PaginatedResponse<AuditLogEntry> getAuditLog(
            String username, String eventTypeFilter, int page, int size);
}
