package com.coagent4u.user.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.events.UserDeleted;
import com.coagent4u.common.events.UserRegistered;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

/**
 * Aggregate root for the User bounded context.
 *
 * <p>
 * All state changes go through this class. Domain events are accumulated
 * and published by the application service after a successful persistence
 * operation.
 *
 * <p>
 * Invariants: a user must always have a Slack identity. A user cannot act
 * after being deleted (soft-deleted via deletedAt timestamp).
 */
public class User {

    private final UserId userId;
    private final String username;
    private Email email;
    private SlackIdentity slackIdentity;
    private final List<ServiceConnection> serviceConnections = new ArrayList<>();
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    // Accumulated domain events — cleared after publishing
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private User(UserId userId, String username, Email email, SlackIdentity slackIdentity) {
        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.email = email;
        this.slackIdentity = Objects.requireNonNull(slackIdentity);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Factory method — creates and registers a new user.
     * Accumulates a {@link UserRegistered} event.
     */
    public static User register(UserId userId, String username, Email email,
            SlackUserId slackUserId, WorkspaceId workspaceId,
            String workspaceName, String workspaceDomain, String slackEmail, String displayName, String avatarUrl) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (!username.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Username must match ^[a-z0-9_]+$");
        }
        SlackIdentity identity = SlackIdentity.of(slackUserId, workspaceId, workspaceName, workspaceDomain, slackEmail, displayName, avatarUrl);
        User user = new User(userId, username, email, identity);
        user.domainEvents.add(UserRegistered.of(userId, username, email, slackUserId, workspaceId));
        return user;
    }

    /**
     * Reconnects or updates a service connection (e.g. Google Calendar OAuth).
     */
    public void connectService(String serviceType, String encryptedToken,
            String encryptedRefreshToken, Instant tokenExpiresAt) {
        requireNotDeleted();
        // Revoke any existing active connection for this service
        serviceConnections.stream()
                .filter(c -> c.getServiceType().equals(serviceType) && c.isActive())
                .forEach(ServiceConnection::revoke);

        serviceConnections.add(new ServiceConnection(
                UUID.randomUUID(), serviceType, encryptedToken, encryptedRefreshToken, tokenExpiresAt));
        this.updatedAt = Instant.now();
    }

    /**
     * Revokes the active service connection for the given service type.
     */
    public void disconnectService(String serviceType) {
        requireNotDeleted();
        serviceConnections.stream()
                .filter(c -> c.getServiceType().equals(serviceType) && c.isActive())
                .forEach(ServiceConnection::revoke);
        this.updatedAt = Instant.now();
    }

    /**
     * Soft-deletes this user. Accumulates a {@link UserDeleted} event.
     */
    public void delete() {
        requireNotDeleted();
        this.deletedAt = Instant.now();
        this.updatedAt = this.deletedAt;
        domainEvents.add(UserDeleted.of(userId));
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Optional<ServiceConnection> activeConnectionFor(String serviceType) {
        return serviceConnections.stream()
                .filter(c -> c.getServiceType().equals(serviceType) && c.isActive())
                .findFirst();
    }

    /** Returns and clears accumulated domain events. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void requireNotDeleted() {
        if (isDeleted())
            throw new IllegalStateException("User " + userId + " is already deleted");
    }

    // Getters
    public UserId getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public SlackIdentity getSlackIdentity() {
        return slackIdentity;
    }

    public List<ServiceConnection> getServiceConnections() {
        return Collections.unmodifiableList(serviceConnections);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
