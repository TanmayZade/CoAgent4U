package com.coagent4u.user.port.out;

import java.util.Optional;

import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;

/**
 * Outbound port — read-only queries for the User aggregate.
 * Used by other modules (e.g. agent-module) that need user data without owning
 * it.
 * Implemented in the persistence module.
 */
public interface UserQueryPort {
    Optional<User> findById(UserId userId);

    boolean existsById(UserId userId);
}
