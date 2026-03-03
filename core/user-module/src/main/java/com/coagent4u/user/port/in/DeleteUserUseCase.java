package com.coagent4u.user.port.in;

import com.coagent4u.shared.UserId;

/**
 * Inbound port — soft-deletes a user and triggers GDPR data purge flow.
 */
public interface DeleteUserUseCase {
    /**
     * @param userId the user to delete
     * @throws IllegalStateException if the user is already deleted
     */
    void delete(UserId userId);
}
