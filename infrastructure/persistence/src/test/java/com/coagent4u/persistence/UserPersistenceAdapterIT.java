package com.coagent4u.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.coagent4u.persistence.user.ServiceConnectionJpaEntity;
import com.coagent4u.persistence.user.SlackIdentityJpaEntity;
import com.coagent4u.persistence.user.UserJpaEntity;
import com.coagent4u.persistence.user.UserJpaRepository;

/**
 * Integration test for User persistence using real PostgreSQL via
 * Testcontainers.
 * Verifies: Flyway migrations applied, save/find round-trip, soft-delete,
 * service connections.
 */
class UserPersistenceAdapterIT extends PostgresIntegrationTest {

    @Autowired
    private UserJpaRepository userRepo;

    @Test
    void saveAndFindById_roundTrip() {
        UUID userId = UUID.randomUUID();
        UserJpaEntity entity = createTestUser(userId, "alice_it");
        userRepo.save(entity);

        Optional<UserJpaEntity> found = userRepo.findById(userId);
        assertTrue(found.isPresent());
        assertEquals("alice_it", found.get().getUsername());
        assertEquals("alice_it@example.com", found.get().getEmail());
        assertNotNull(found.get().getSlackIdentity());
    }

    @Test
    void findBySlackUserId() {
        String slackId = "U_BOB_" + UUID.randomUUID().toString().substring(0, 8);
        UUID userId = UUID.randomUUID();
        UserJpaEntity entity = createTestUserWithSlack(userId, "bob_it_" + userId.toString().substring(0, 4), slackId);
        userRepo.save(entity);

        Optional<UserJpaEntity> found = userRepo
                .findBySlackIdentity_SlackUserIdAndSlackIdentity_WorkspaceId(slackId, "W_TEST");
        assertTrue(found.isPresent());
    }

    @Test
    void existsByUsername() {
        String username = "charlie_it_" + UUID.randomUUID().toString().substring(0, 8);
        UUID userId = UUID.randomUUID();
        UserJpaEntity entity = createTestUser(userId, username);
        userRepo.save(entity);

        assertTrue(userRepo.existsByUsername(username));
        assertFalse(userRepo.existsByUsername("nonexistent_user_xyz"));
    }

    @Test
    void deleteById() {
        UUID userId = UUID.randomUUID();
        UserJpaEntity entity = createTestUser(userId, "dave_it_" + userId.toString().substring(0, 4));
        userRepo.save(entity);
        assertTrue(userRepo.existsById(userId));

        userRepo.deleteById(userId);
        assertFalse(userRepo.existsById(userId));
    }

    @Test
    @Transactional
    void serviceConnection_persistsCorrectly() {
        UUID userId = UUID.randomUUID();
        UserJpaEntity entity = createTestUser(userId, "eve_it_" + userId.toString().substring(0, 4));

        ServiceConnectionJpaEntity conn = new ServiceConnectionJpaEntity(
                UUID.randomUUID(),
                "GOOGLE_CALENDAR",
                "encrypted-token-data",
                "encrypted-refresh-data",
                Instant.now().plusSeconds(3600),
                "CONNECTED");
        conn.setUser(entity);
        entity.getServiceConnections().add(conn);

        userRepo.save(entity);

        UserJpaEntity loaded = userRepo.findById(userId).orElseThrow();
        assertEquals(1, loaded.getServiceConnections().size());
        assertEquals("GOOGLE_CALENDAR", loaded.getServiceConnections().get(0).getServiceType());
        assertEquals("encrypted-token-data", loaded.getServiceConnections().get(0).getEncryptedToken());
    }

    private UserJpaEntity createTestUser(UUID userId, String username) {
        String slackId = "U_" + UUID.randomUUID().toString().substring(0, 8);
        return createTestUserWithSlack(userId, username, slackId);
    }

    private UserJpaEntity createTestUserWithSlack(UUID userId, String username, String slackId) {
        UserJpaEntity entity = new UserJpaEntity(
                userId, username, username + "@example.com",
                Instant.now(), Instant.now(), null);
        // SlackIdentityJpaEntity(String slackUserId, String workspaceId, String
        // displayName)
        SlackIdentityJpaEntity slack = new SlackIdentityJpaEntity(slackId, "W_TEST", username);
        entity.setSlackIdentity(slack);
        return entity;
    }
}
