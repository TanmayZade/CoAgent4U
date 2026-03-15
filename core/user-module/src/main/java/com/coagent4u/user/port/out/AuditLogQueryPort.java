package com.coagent4u.user.port.out;

import java.util.List;

import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AuditLogEntry;

/**
 * Outbound port — read-only queries for audit log data.
 * Implemented in the persistence module.
 */
public interface AuditLogQueryPort {
    List<AuditLogEntry> findByUserId(UserId userId, String eventTypeFilter, int offset, int limit);

    long countByUserId(UserId userId, String eventTypeFilter);

    List<AuditLogEntry> findAllByUserId(UserId userId);
}
