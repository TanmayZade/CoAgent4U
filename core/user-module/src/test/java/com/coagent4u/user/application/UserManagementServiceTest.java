package com.coagent4u.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;

class UserManagementServiceTest {

    private UserPersistencePort persistence;
    private DomainEventPublisher eventPublisher;
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        persistence = mock(UserPersistencePort.class);
        eventPublisher = mock(DomainEventPublisher.class);
        service = new UserManagementService(persistence, eventPublisher);
    }

    @Test
    void register_success() {
        when(persistence.existsByUsername("alice_dev")).thenReturn(false);
        when(persistence.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.register(UserId.generate(), "alice_dev", Email.of("a@b.com"),
                SlackUserId.of("U1"), WorkspaceId.of("T1"), null, null, null, null, null);

        verify(persistence).save(any(User.class));
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void register_duplicateUsername_rejects() {
        when(persistence.existsByUsername("taken")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.register(UserId.generate(), "taken", Email.of("a@b.com"),
                        SlackUserId.of("U1"), WorkspaceId.of("T1"), null, null, null, null, null));
    }

    @Test
    void connect_success() {
        UserId uid = UserId.generate();
        User user = User.register(uid, "bob", Email.of("b@c.com"),
                SlackUserId.of("U2"), WorkspaceId.of("T2"), null, null, null, null, null);
        user.pullDomainEvents();
        when(persistence.findById(uid)).thenReturn(Optional.of(user));
        when(persistence.save(any())).thenAnswer(i -> i.getArgument(0));

        service.connect(uid, "GOOGLE_CALENDAR", "enc", "ref", Instant.now().plusSeconds(3600));
        verify(persistence).save(any(User.class));
    }

    @Test
    void delete_success() {
        UserId uid = UserId.generate();
        User user = User.register(uid, "charlie", Email.of("c@d.com"),
                SlackUserId.of("U3"), WorkspaceId.of("T3"), null, null, null, null, null);
        user.pullDomainEvents();
        when(persistence.findById(uid)).thenReturn(Optional.of(user));
        when(persistence.save(any())).thenAnswer(i -> i.getArgument(0));

        service.delete(uid);
        verify(eventPublisher).publish(any(DomainEvent.class));
        assertTrue(user.isDeleted());
    }

    @Test
    void delete_notFound_throws() {
        UserId uid = UserId.generate();
        when(persistence.findById(uid)).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.delete(uid));
    }
}
