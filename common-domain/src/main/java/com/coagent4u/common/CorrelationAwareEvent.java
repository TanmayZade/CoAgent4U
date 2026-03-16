package com.coagent4u.common;

import com.coagent4u.shared.CorrelationId;

/**
 * Marker interface for domain events that are part of a specific workflow trace.
 * This allows the {@code AgentActivityEventHandler} to group related events together
 * into a single logical "session".
 */
public interface CorrelationAwareEvent extends DomainEvent {

    /**
     * The correlation ID tracking the workflow that generated this event.
     */
    CorrelationId correlationId();
}
