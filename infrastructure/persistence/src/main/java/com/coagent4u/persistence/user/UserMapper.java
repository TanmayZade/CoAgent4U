package com.coagent4u.persistence.user;

import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;

/**
 * Infrastructure-only mapper between User domain aggregate and JPA entities.
 * Uses public API + {@code pullDomainEvents()} to discard spurious events.
 * This is an infrastructure concern — no domain contamination.
 */
public final class UserMapper {

        private UserMapper() {
        }

        public static UserJpaEntity toJpa(User user) {
                UserJpaEntity entity = new UserJpaEntity();
                entity.setUserId(user.getUserId().value());
                entity.setUsername(user.getUsername());
                entity.setEmail(user.getEmail().value());
                entity.setCreatedAt(user.getCreatedAt());
                entity.setUpdatedAt(user.getUpdatedAt());
                entity.setDeletedAt(user.getDeletedAt());

                // Slack identity
                SlackIdentityJpaEntity slack = new SlackIdentityJpaEntity();
                slack.setSlackUserId(user.getSlackIdentity().slackUserId().value());
                slack.setWorkspaceId(user.getSlackIdentity().workspaceId().value());
                slack.setUser(entity);
                entity.setSlackIdentity(slack);

                // Service connections — entity uses ManyToOne to UserJpaEntity, no setUserId()
                for (var conn : user.getServiceConnections()) {
                        ServiceConnectionJpaEntity ce = new ServiceConnectionJpaEntity();
                        ce.setConnectionId(conn.getConnectionId());
                        ce.setUser(entity); // ManyToOne association, not raw UUID
                        ce.setServiceType(conn.getServiceType());
                        ce.setEncryptedToken(conn.getEncryptedToken());
                        ce.setEncryptedRefreshToken(conn.getEncryptedRefreshToken());
                        ce.setTokenExpiresAt(conn.getTokenExpiresAt());
                        ce.setStatus(conn.getStatus().name());
                        ce.setConnectedAt(conn.getConnectedAt());
                        entity.getServiceConnections().add(ce);
                }
                return entity;
        }

        /**
         * Reconstitutes User from JPA using public factory + pullDomainEvents().
         * Timestamps may differ from DB (MVP-acceptable); JPA entity is authoritative
         * snapshot.
         */
        public static User toDomain(UserJpaEntity e) {
                // 1. Create via public factory
                User user = User.register(
                                new UserId(e.getUserId()),
                                e.getUsername(),
                                Email.of(e.getEmail()),
                                SlackUserId.of(e.getSlackIdentity().getSlackUserId()),
                                WorkspaceId.of(e.getSlackIdentity().getWorkspaceId()));
                // 2. Discard the spurious UserRegistered event
                user.pullDomainEvents();

                // 3. Reconnect service connections via public API
                for (ServiceConnectionJpaEntity ce : e.getServiceConnections()) {
                        if ("CONNECTED".equals(ce.getStatus())) {
                                user.connectService(
                                                ce.getServiceType(),
                                                ce.getEncryptedToken(),
                                                ce.getEncryptedRefreshToken(),
                                                ce.getTokenExpiresAt());
                                user.pullDomainEvents(); // discard spurious events
                        }
                }

                // 4. If soft-deleted, call delete() and discard event
                if (e.getDeletedAt() != null) {
                        user.delete();
                        user.pullDomainEvents();
                }

                return user;
        }
}
