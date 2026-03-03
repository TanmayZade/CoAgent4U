package com.coagent4u.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Publishes Micrometer metrics for domain events.
 * Handler failure is caught — never propagated to coordination transaction.
 */
@Component
public class MetricsEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MetricsEventHandler.class);

    private final MeterRegistry registry;

    public MetricsEventHandler(MeterRegistry registry) {
        this.registry = registry;
    }

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        try {
            Counter.builder("coagent.domain_events")
                    .tag("type", event.getClass().getSimpleName())
                    .register(registry)
                    .increment();
        } catch (Exception e) {
            log.error("Metrics recording failed for event {} — coordination unaffected",
                    event.eventId(), e);
        }
    }
}
