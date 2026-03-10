package com.coagent4u.app.rest;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.app.security.AuthenticatedUser;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;

import jakarta.servlet.http.HttpServletRequest;

/**
 * User profile endpoint.
 * Returns authenticated user's profile data.
 */
@RestController
public class UserController {

    private final UserPersistencePort userPersistencePort;

    public UserController(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
    }

    /**
     * GET /me — Returns authenticated user's profile.
     * Frontend uses username; backend uses userId internally.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        AuthenticatedUser authUser = AuthenticatedUser.from(request);
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        Optional<User> userOpt = userPersistencePort.findById(new UserId(authUser.userId()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "userId", authUser.userId().toString(),
                    "username", authUser.username() != null ? authUser.username() : "",
                    "pendingRegistration", authUser.pendingRegistration()));
        }

        User user = userOpt.get();
        boolean googleConnected = user.activeConnectionFor("GOOGLE_CALENDAR").isPresent();

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId().value().toString(),
                "username", user.getUsername(),
                "email", user.getEmail() != null ? user.getEmail().value() : "",
                "googleCalendarConnected", googleConnected,
                "createdAt", user.getCreatedAt().toString()));
    }
}
