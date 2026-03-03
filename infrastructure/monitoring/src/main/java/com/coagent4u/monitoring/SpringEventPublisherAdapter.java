package com.coagent4u.monitoring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.DomainEventPublisher;

/**
 * Implements {@link DomainEventPublisher} by delegating to Spring's
 * {@link ApplicationEventPublisher}. Events are dispatched asynchronously
 * to all registered {@code @EventListener} handlers.
 */
@Component
public class SpringEventPublisherAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
