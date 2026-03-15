package com.coagent4u.persistence.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * Implements UserQueryPort ONLY (findById, existsById).
 * Separated from UserPersistenceAdapter — one adapter per port.
 */
@Component
public class UserQueryAdapter implements UserQueryPort {

    private final UserJpaRepository repository;

    public UserQueryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return repository.findById(userId.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsById(UserId userId) {
        return repository.existsById(userId.value());
    }
}
