package com.coagent4u.app.rest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.app.security.AuthenticatedUser;
import com.coagent4u.app.security.GoogleOAuthStateService;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.ConnectServiceUseCase;
import com.coagent4u.user.port.in.DisconnectServiceUseCase;
import com.coagent4u.user.port.out.OAuthTokenExchangePort;
import com.coagent4u.user.port.out.OAuthTokenExchangePort.OAuthTokenResult;
import com.coagent4u.user.port.out.UserPersistencePort;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Google Calendar integration controller.
 * Handles OAuth authorization, callback, disconnection, and status.
 *
 * <p>State parameter uses signed JWT to prevent CSRF attacks.</p>
 */
@RestController
@RequestMapping("/integrations/google")
public class IntegrationController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);

    private final CoagentProperties properties;
    private final ConnectServiceUseCase connectServiceUseCase;
    private final DisconnectServiceUseCase disconnectServiceUseCase;
    private final OAuthTokenExchangePort oAuthTokenExchangePort;
    private final UserPersistencePort userPersistencePort;
    private final GoogleOAuthStateService stateService;

    public IntegrationController(
            CoagentProperties properties,
            ConnectServiceUseCase connectServiceUseCase,
            DisconnectServiceUseCase disconnectServiceUseCase,
            OAuthTokenExchangePort oAuthTokenExchangePort,
            UserPersistencePort userPersistencePort,
            GoogleOAuthStateService stateService) {
        this.properties = properties;
        this.connectServiceUseCase = connectServiceUseCase;
        this.disconnectServiceUseCase = disconnectServiceUseCase;
        this.oAuthTokenExchangePort = oAuthTokenExchangePort;
        this.userPersistencePort = userPersistencePort;
        this.stateService = stateService;
    }

    /**
     * GET /integrations/google/authorize — Redirects to Google OAuth consent screen.
     * State parameter is a signed JWT containing userId + nonce + timestamp.
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String stateToken = stateService.createStateToken(user.userId());

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + properties.getGoogle().getClientId()
                + "&redirect_uri=" + properties.getGoogle().getRedirectUri()
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/calendar.events"
                + "+https://www.googleapis.com/auth/calendar.readonly"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + stateToken;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    /**
     * GET /integrations/google/callback — Handles Google OAuth callback.
     * Validates signed state token before exchanging code.
     */
    @GetMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state) {

        if (error != null) {
            log.warn("Google OAuth callback error: {}", error);
            return ResponseEntity.badRequest().body("OAuth error: " + error);
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing authorization code");
        }
        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().body("Missing state parameter");
        }

        // Validate signed state token
        UUID userId = stateService.validateStateToken(state);
        if (userId == null) {
            log.warn("Invalid or expired Google OAuth state token");
            return ResponseEntity.badRequest().body("Invalid or expired state token");
        }

        try {
            // Exchange code for encrypted tokens
            OAuthTokenResult tokens = oAuthTokenExchangePort.exchangeCode(code);

            // Store encrypted tokens via ConnectServiceUseCase
            connectServiceUseCase.connect(
                    new UserId(userId),
                    "GOOGLE_CALENDAR",
                    tokens.encryptedAccessToken(),
                    tokens.encryptedRefreshToken(),
                    tokens.expiresAt());

            log.info("Google Calendar connected for userId={}", userId);
            return ResponseEntity.ok("Google Calendar connected successfully! You can close this window.");

        } catch (Exception e) {
            log.error("Google OAuth callback failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect Google Calendar: " + e.getMessage());
        }
    }

    /**
     * DELETE /integrations/google/disconnect — Revokes Google Calendar connection.
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        try {
            disconnectServiceUseCase.disconnect(new UserId(user.userId()), "GOOGLE_CALENDAR");
            log.info("Google Calendar disconnected for userId={}", user.userId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Google Calendar disconnected"));
        } catch (Exception e) {
            log.error("Google Calendar disconnect failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to disconnect: " + e.getMessage()));
        }
    }

    /**
     * GET /integrations/google/status — Returns Google Calendar connection status.
     */
    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }

        Optional<User> userOpt = userPersistencePort.findById(new UserId(user.userId()));
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        boolean connected = userOpt.get().activeConnectionFor("GOOGLE_CALENDAR").isPresent();
        return ResponseEntity.ok(Map.of(
                "service", "GOOGLE_CALENDAR",
                "connected", connected));
    }
}
