package com.coagent4u.monitoring;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.persistence.audit.AuditLogJpaEntity;
import com.coagent4u.persistence.audit.AuditLogJpaRepository;

/**
 * Persists all domain events to the audit_logs table for compliance.
 * Handler failure is caught and logged — never propagated to coordination
 * transaction.
 */
@Component
public class AuditEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditEventHandler.class);

    private final AuditLogJpaRepository auditRepo;

    public AuditEventHandler(AuditLogJpaRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        try {
            AuditLogJpaEntity auditLog = new AuditLogJpaEntity(
                    UUID.randomUUID(), // Infrastructure surrogate key (not domain UUID)
                    null, // userId extracted per event type in Phase 3
                    event.getClass().getSimpleName(),
                    null, // payload serialized in Phase 3
                    null,
                    event.occurredAt() // Domain timestamp — NOT Instant.now()
            );
            auditRepo.save(auditLog);
        } catch (Exception e) {
            log.error("Audit write failed for event {} — coordination unaffected",
                    event.eventId(), e);
        }
    }
}
