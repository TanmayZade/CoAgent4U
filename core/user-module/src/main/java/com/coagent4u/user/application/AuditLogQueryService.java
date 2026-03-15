package com.coagent4u.user.application;

import java.util.List;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AuditLogEntry;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.GetAuditLogUseCase;
import com.coagent4u.user.port.out.AuditLogQueryPort;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Application service for audit log queries.
 * No Spring annotations — assembled by DI in coagent-app.
 */
public class AuditLogQueryService implements GetAuditLogUseCase {

    private final UserQueryPort userQuery;
    private final AuditLogQueryPort auditLogQuery;

    public AuditLogQueryService(UserQueryPort userQuery, AuditLogQueryPort auditLogQuery) {
        this.userQuery = userQuery;
        this.auditLogQuery = auditLogQuery;
    }

    @Override
    public PaginatedResponse<AuditLogEntry> getAuditLog(
            String username, String eventTypeFilter, int page, int size) {
        User user = userQuery.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

        UserId userId = user.getUserId();
        int offset = page * size;

        List<AuditLogEntry> entries = auditLogQuery.findByUserId(userId, eventTypeFilter, offset, size);
        long total = auditLogQuery.countByUserId(userId, eventTypeFilter);

        return new PaginatedResponse<>(entries, page, size, total,
                (int) Math.ceil((double) total / size));
    }
}
