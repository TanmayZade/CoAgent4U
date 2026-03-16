package com.coagent4u.common;

import com.coagent4u.shared.UserId;

/**
 * Marker interface for domain events that carry a user identity.
 * Implemented by agent activity events so the {@code AgentActivityEventHandler}
 * can extract the {@code userId} and bind audit rows to the correct user.
 */
public interface UserAwareEvent extends DomainEvent {

    /**
     * The user on whose behalf this event occurred.
     */
    UserId userId();
}
