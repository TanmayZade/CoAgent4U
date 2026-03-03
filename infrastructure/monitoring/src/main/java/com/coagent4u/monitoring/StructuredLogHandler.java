package com.coagent4u.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;

/**
 * Logs all domain events in a structured format for observability.
 * Handler failure is caught — never propagated to coordination transaction.
 */
@Component
public class StructuredLogHandler {

    private static final Logger log = LoggerFactory.getLogger(StructuredLogHandler.class);

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        try {
            log.info("domain_event={{type={}, eventId={}, occurredAt={}}}",
                    event.getClass().getSimpleName(),
                    event.eventId(),
                    event.occurredAt());
        } catch (Exception e) {
            log.error("Structured logging failed for event — coordination unaffected", e);
        }
    }
}
