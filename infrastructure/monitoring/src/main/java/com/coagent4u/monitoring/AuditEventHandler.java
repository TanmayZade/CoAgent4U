package com.coagent4u.monitoring;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.UserAwareEvent;
import com.coagent4u.persistence.audit.AuditLogJpaEntity;
import com.coagent4u.persistence.audit.AuditLogJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Persists all domain events to the audit_logs table for compliance.
 * Handler failure is caught and logged — never propagated to coordination
 * transaction.
 *
 * <p>Phase 3 complete:
 * <ul>
 *   <li>Extracts {@code userId} from events implementing {@link UserAwareEvent}</li>
 *   <li>Serializes the full event payload to JSON via Jackson</li>
 * </ul>
 */
@Component
public class AuditEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditEventHandler.class);

    private final AuditLogJpaRepository auditRepo;
    private final ObjectMapper objectMapper;

    public AuditEventHandler(AuditLogJpaRepository auditRepo) {
        this.auditRepo = auditRepo;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        try {
            // Phase 3a: Extract userId if event is user-aware
            UUID userId = null;
            if (event instanceof UserAwareEvent userEvent) {
                userId = userEvent.userId().value();
            }

            // Phase 3b: Serialize full event payload to JSON
            String payloadJson = null;
            try {
                payloadJson = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                log.warn("Failed to serialize event payload for {}: {}",
                        event.getClass().getSimpleName(), e.getMessage());
            }

            AuditLogJpaEntity auditLog = new AuditLogJpaEntity(
                    UUID.randomUUID(),
                    userId,
                    event.getClass().getSimpleName(),
                    payloadJson,
                    null,
                    event.occurredAt()
            );
            auditRepo.save(auditLog);
        } catch (Exception e) {
            log.error("Audit write failed for event {} — coordination unaffected",
                    event.eventId(), e);
        }
    }
}
