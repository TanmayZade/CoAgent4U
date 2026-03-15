package com.coagent4u.persistence.audit;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.coagent4u.shared.UserId;
import com.coagent4u.user.application.dto.AuditLogEntry;
import com.coagent4u.user.port.out.AuditLogQueryPort;

/**
 * Persistence adapter implementing AuditLogQueryPort.
 */
@Component
public class AuditLogQueryAdapter implements AuditLogQueryPort {

    private final AuditLogJpaRepository repository;

    public AuditLogQueryAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AuditLogEntry> findByUserId(UserId userId, String eventTypeFilter, int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        PageRequest pageRequest = PageRequest.of(page, limit);

        Page<AuditLogJpaEntity> result;
        if (eventTypeFilter != null && !eventTypeFilter.isBlank()) {
            result = repository.findByUserIdAndEventTypeOrderByOccurredAtDesc(
                    userId.value(), eventTypeFilter, pageRequest);
        } else {
            result = repository.findByUserIdOrderByOccurredAtDesc(userId.value(), pageRequest);
        }

        return result.getContent().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long countByUserId(UserId userId, String eventTypeFilter) {
        if (eventTypeFilter != null && !eventTypeFilter.isBlank()) {
            return repository.countByUserIdAndEventType(userId.value(), eventTypeFilter);
        }
        return repository.countByUserId(userId.value());
    }

    @Override
    public List<AuditLogEntry> findAllByUserId(UserId userId) {
        return repository.findByUserIdOrderByOccurredAtDesc(userId.value()).stream()
                .map(this::toDto)
                .toList();
    }

    private AuditLogEntry toDto(AuditLogJpaEntity entity) {
        return new AuditLogEntry(
                entity.getLogId(),
                entity.getEventType(),
                entity.getPayloadJson(),
                entity.getCorrelationId(),
                entity.getOccurredAt());
    }
}
