package com.coagent4u.user.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.coagent4u.common.DomainEvent;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;

class UserTest {

    private static final UserId UID = UserId.generate();
    private static final Email EMAIL = Email.of("alice@test.com");
    private static final SlackUserId SLACK = SlackUserId.of("U01ABC");
    private static final WorkspaceId WS = WorkspaceId.of("T01XYZ");

    @Test
    void register_createsUserWithEvent() {
        User user = User.register(UID, "alice_dev", EMAIL, SLACK, WS);
        assertEquals("alice_dev", user.getUsername());
        assertEquals(SLACK, user.getSlackIdentity().slackUserId());
        assertFalse(user.isDeleted());
        List<DomainEvent> events = user.pullDomainEvents();
        assertEquals(1, events.size());
        assertEquals("com.coagent4u.common.events.UserRegistered", events.get(0).getClass().getName());
    }

    @Test
    void register_blankUsernameRejects() {
        assertThrows(IllegalArgumentException.class,
                () -> User.register(UID, "", EMAIL, SLACK, WS));
    }

    @Test
    void register_invalidUsernameRejects() {
        assertThrows(IllegalArgumentException.class,
                () -> User.register(UID, "Alice Dev!", EMAIL, SLACK, WS));
    }

    @Test
    void connectService_addsConnection() {
        User user = User.register(UID, "bob", EMAIL, SLACK, WS);
        user.pullDomainEvents(); // clear
        user.connectService("GOOGLE_CALENDAR", "enc_token", "enc_refresh", Instant.now().plusSeconds(3600));
        assertEquals(1, user.getServiceConnections().size());
        assertTrue(user.getServiceConnections().get(0).isActive());
    }

    @Test
    void disconnectService_revokesConnection() {
        User user = User.register(UID, "charlie", EMAIL, SLACK, WS);
        user.connectService("GOOGLE_CALENDAR", "t", "r", Instant.now().plusSeconds(3600));
        user.disconnectService("GOOGLE_CALENDAR");
        assertFalse(user.getServiceConnections().get(0).isActive());
    }

    @Test
    void delete_softDeletes() {
        User user = User.register(UID, "dave", EMAIL, SLACK, WS);
        user.pullDomainEvents(); // clear registration event
        user.delete();
        assertTrue(user.isDeleted());
        assertNotNull(user.getDeletedAt());
        assertEquals(1, user.pullDomainEvents().size()); // UserDeleted
    }

    @Test
    void delete_alreadyDeleted_rejects() {
        User user = User.register(UID, "eve", EMAIL, SLACK, WS);
        user.delete();
        assertThrows(IllegalStateException.class, user::delete);
    }

    @Test
    void connectService_afterDelete_rejects() {
        User user = User.register(UID, "frank", EMAIL, SLACK, WS);
        user.delete();
        assertThrows(IllegalStateException.class,
                () -> user.connectService("GOOGLE_CALENDAR", "t", "r", Instant.now()));
    }

    @Test
    void pullDomainEvents_clearsAfterPull() {
        User user = User.register(UID, "grace", EMAIL, SLACK, WS);
        assertEquals(1, user.pullDomainEvents().size());
        assertEquals(0, user.pullDomainEvents().size()); // already cleared
    }
}
