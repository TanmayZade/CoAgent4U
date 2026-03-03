package com.coagent4u.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the CoAgent4U system.
 * Domain events are immutable records of something that has already happened.
 * They are published after state-changing operations and consumed
 * asynchronously.
 */
public interface DomainEvent {

    /**
     * Unique identifier for this specific event occurrence.
     * Default implementation generates a random UUID per call.
     * Override in event records if a stable identity is needed.
     */
    default UUID eventId() {
        return UUID.randomUUID();
    }

    /**
     * When this event occurred.
     */
    Instant occurredAt();
}
