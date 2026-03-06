package com.coagent4u.persistence.user;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Implements UserPersistencePort ONLY.
 * Read-only queries live in UserQueryAdapter — one adapter per port.
 */
@Component
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserMapper.toJpa(user);
        repository.save(entity);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return repository.findById(userId.value()).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findBySlackUserId(SlackUserId slackUserId, WorkspaceId workspaceId) {
        return repository.findBySlackIdentity_SlackUserIdAndSlackIdentity_WorkspaceId(
                slackUserId.value(), workspaceId.value()).map(UserMapper::toDomain);
    }

    @Override
    public void delete(UserId userId) {
        repository.deleteById(userId.value());
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(UserMapper::toDomain);
    }
}
