package com.coagent4u.user.application;

import java.time.Instant;
import java.util.NoSuchElementException;

import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.ConnectServiceUseCase;
import com.coagent4u.user.port.in.DeleteUserUseCase;
import com.coagent4u.user.port.in.DisconnectServiceUseCase;
import com.coagent4u.user.port.in.RegisterUserUseCase;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Application service for the User bounded context.
 * Implements all inbound use case ports — orchestrates domain logic and
 * publishes events.
 *
 * <p>
 * This class has zero Spring annotations — it is assembled by the DI container
 * in
 * coagent-app. Domain purity is preserved here intentionally.
 */
public class UserManagementService
        implements RegisterUserUseCase, ConnectServiceUseCase,
        DisconnectServiceUseCase, DeleteUserUseCase {

    private final UserPersistencePort persistence;
    private final DomainEventPublisher eventPublisher;

    public UserManagementService(UserPersistencePort persistence,
            DomainEventPublisher eventPublisher) {
        this.persistence = persistence;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void register(UserId userId, String username, Email email,
            SlackUserId slackUserId, WorkspaceId workspaceId,
            String workspaceName, String workspaceDomain, String slackEmail, String displayName, String avatarUrl) {
        if (persistence.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        User user = User.register(userId, username, email, slackUserId, workspaceId, 
                workspaceName, workspaceDomain, slackEmail, displayName, avatarUrl);
        persistence.save(user);
        publishAndClear(user);
    }

    @Override
    public void connect(UserId userId, String serviceType,
            String encryptedToken, String encryptedRefreshToken, Instant tokenExpiresAt) {
        User user = loadUser(userId);
        user.connectService(serviceType, encryptedToken, encryptedRefreshToken, tokenExpiresAt);
        persistence.save(user);
        publishAndClear(user);
    }

    @Override
    public void disconnect(UserId userId, String serviceType) {
        User user = loadUser(userId);
        user.disconnectService(serviceType);
        persistence.save(user);
        publishAndClear(user);
    }

    @Override
    public void delete(UserId userId) {
        User user = loadUser(userId);
        user.delete();
        persistence.save(user);
        publishAndClear(user);
    }

    private User loadUser(UserId userId) {
        return persistence.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private void publishAndClear(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publish);
    }
}
