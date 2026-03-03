package com.coagent4u.user.port.out;

import java.util.Optional;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;

/**
 * Outbound port — persistence operations for the User aggregate.
 * Implemented in the persistence module (UserPersistenceAdapter).
 */
public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(UserId userId);

    Optional<User> findBySlackUserId(SlackUserId slackUserId, WorkspaceId workspaceId);

    void delete(UserId userId);

    boolean existsByUsername(String username);
}
