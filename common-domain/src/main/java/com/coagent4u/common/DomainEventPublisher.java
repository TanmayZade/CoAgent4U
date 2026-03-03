package com.coagent4u.common;

/**
 * Outbound port for publishing domain events.
 * Implemented by the infrastructure layer (in-memory Spring event bus).
 *
 * <p>Published events are dispatched asynchronously. Consumer failures
 * must never affect the outcome of the operation that published the event.</p>
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event to all registered async consumers.
     *
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);
}
