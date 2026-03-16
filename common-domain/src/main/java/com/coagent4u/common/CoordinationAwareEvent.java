package com.coagent4u.common;

import com.coagent4u.shared.CoordinationId;

/**
 * Marker interface for domain events that are part of an Agent-to-Agent coordination flow.
 * This allows the {@code AgentActivityEventHandler} to tag agent activitys with the specific
 * negotiation context.
 */
public interface CoordinationAwareEvent extends DomainEvent {

    /**
     * The coordination ID identifying the collaborative scheduling flow.
     */
    CoordinationId coordinationId();
}
