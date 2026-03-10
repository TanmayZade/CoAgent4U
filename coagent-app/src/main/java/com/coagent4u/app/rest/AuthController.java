package com.coagent4u.app.rest;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.app.security.AuthenticatedUser;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.security.JwtIssuer;
import com.coagent4u.security.JwtTokenBlacklist;
import com.coagent4u.security.JwtValidator;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.RegisterUserUseCase;
import com.coagent4u.user.port.out.SlackOAuthPort;
import com.coagent4u.user.port.out.SlackOAuthPort.SlackOAuthResult;
import com.coagent4u.user.port.out.UserPersistencePort;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication controller for Slack OAuth login, username registration,
 * logout, and session verification.
 *
 * <p>Frontend uses {@code username}; backend uses {@code userId} internally.
 * Slack identity always uses slackUserId + workspaceId (teamId).</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String COOKIE_NAME = "coagent_session";
    private static final java.util.regex.Pattern USERNAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");

    private final CoagentProperties properties;
    private final SlackOAuthPort slackOAuthPort;
    private final UserPersistencePort userPersistencePort;
    private final RegisterUserUseCase registerUserUseCase;
    private final AgentPersistencePort agentPersistencePort;
    private final JwtIssuer jwtIssuer;
    private final JwtValidator jwtValidator;
    private final JwtTokenBlacklist tokenBlacklist;

    public AuthController(
            CoagentProperties properties,
            SlackOAuthPort slackOAuthPort,
            UserPersistencePort userPersistencePort,
            RegisterUserUseCase registerUserUseCase,
            AgentPersistencePort agentPersistencePort,
            JwtIssuer jwtIssuer,
            JwtValidator jwtValidator,
            JwtTokenBlacklist tokenBlacklist) {
        this.properties = properties;
        this.slackOAuthPort = slackOAuthPort;
        this.userPersistencePort = userPersistencePort;
        this.registerUserUseCase = registerUserUseCase;
        this.agentPersistencePort = agentPersistencePort;
        this.jwtIssuer = jwtIssuer;
        this.jwtValidator = jwtValidator;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * GET /auth/slack/start — Redirects to Slack OAuth consent screen.
     * Uses OpenID Connect scopes: openid, profile, email, users:read, users:read.email.
     */
    @GetMapping("/slack/start")
    public ResponseEntity<Void> slackStart() {
        String authUrl = "https://slack.com/openid/connect/authorize"
                + "?client_id=" + properties.getSlack().getClientId()
                + "&redirect_uri=" + properties.getSlack().getRedirectUri()
                + "&response_type=code"
                + "&scope=openid+profile+email"
                + "&nonce=" + UUID.randomUUID();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    /**
     * GET /auth/slack/callback — Handles Slack OAuth callback.
     * <ol>
     *   <li>Exchanges code for Slack tokens + user identity</li>
     *   <li>Looks up user by slackUserId + workspaceId</li>
     *   <li>Existing user → Issue JWT, redirect to frontend</li>
     *   <li>New user → Issue pending JWT, redirect to onboarding page</li>
     * </ol>
     */
    @GetMapping("/slack/callback")
    public ResponseEntity<Void> slackCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null) {
            log.warn("Slack OAuth error: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION,
                            properties.getFrontendUrl() + "/login?error=" + error)
                    .build();
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION,
                            properties.getFrontendUrl() + "/login?error=missing_code")
                    .build();
        }

        try {
            // Exchange code for Slack identity (uses slackUserId + workspaceId)
            SlackOAuthResult slackResult = slackOAuthPort.exchangeCode(code);

            // Look up existing user by slackUserId + workspaceId
            var existingUser = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackResult.slackUserId()),
                    new WorkspaceId(slackResult.workspaceId()));

            if (existingUser.isPresent()) {
                // Existing user — issue full JWT
                User user = existingUser.get();
                String token = jwtIssuer.issue(
                        user.getUserId().value(),
                        user.getUsername(),
                        false);

                log.info("Slack login successful for existing user={}", user.getUsername());

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, properties.getFrontendUrl() + "/dashboard")
                        .header(HttpHeaders.SET_COOKIE, createSessionCookie(token).toString())
                        .build();
            } else {
                // New user — issue pending registration JWT
                UUID tempUserId = UUID.randomUUID();
                String token = jwtIssuer.issue(tempUserId, null, true);

                // Store Slack identity info in a temporary way via the JWT itself
                // The frontend will call POST /auth/username with their chosen username
                log.info("New Slack user detected: slackUserId={}, workspaceId={}",
                        slackResult.slackUserId(), slackResult.workspaceId());

                // Store slack info in a short-lived server-side mapping
                // For MVP, we embed it in the redirect URL as query params
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION,
                                properties.getFrontendUrl() + "/onboarding"
                                        + "?slackUserId=" + slackResult.slackUserId()
                                        + "&workspaceId=" + slackResult.workspaceId()
                                        + "&email=" + (slackResult.email() != null ? slackResult.email() : "")
                                        + "&displayName=" + (slackResult.displayName() != null ? slackResult.displayName() : ""))
                        .header(HttpHeaders.SET_COOKIE, createSessionCookie(token).toString())
                        .build();
            }

        } catch (Exception e) {
            log.error("Slack OAuth callback failed", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION,
                            properties.getFrontendUrl() + "/login?error=oauth_failed")
                    .build();
        }
    }

    /**
     * POST /auth/username — Completes onboarding for new users.
     * <ol>
     *   <li>Validates username format: {@code ^[a-zA-Z0-9_-]{3,32}$}</li>
     *   <li>Creates User via RegisterUserUseCase</li>
     *   <li>Provisions Agent (idempotent) in service layer</li>
     *   <li>Reissues JWT with full claims</li>
     * </ol>
     */
    @PostMapping("/username")
    public ResponseEntity<?> submitUsername(
            @RequestBody UsernameRequest request,
            HttpServletRequest httpRequest) {

        AuthenticatedUser authUser = AuthenticatedUser.from(httpRequest);
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required"));
        }
        if (!authUser.pendingRegistration()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User already registered"));
        }

        // Validate username
        String username = request.username();
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username must match ^[a-zA-Z0-9_-]{3,32}$"));
        }

        try {
            // Create user via use case
            UserId userId = new UserId(UUID.randomUUID());
            Email email = (request.email() != null && !request.email().isBlank())
                    ? new Email(request.email()) : new Email(username + "@coagent4u.local");

            registerUserUseCase.register(
                    userId,
                    username.toLowerCase(),
                    email,
                    new SlackUserId(request.slackUserId()),
                    new WorkspaceId(request.workspaceId()));

            // Provision agent (idempotent)
            provisionAgent(userId);

            // Blacklist the old pending JWT
            String oldToken = extractTokenFromCookie(httpRequest);
            if (oldToken != null) {
                jwtValidator.validateFull(oldToken).ifPresent(claims -> {
                    if (claims.jti() != null) {
                        tokenBlacklist.revoke(claims.jti());
                    }
                });
            }

            // Issue new JWT with full claims
            String newToken = jwtIssuer.issue(userId.value(), username.toLowerCase(), false);

            log.info("User registered: username={}, userId={}", username, userId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, createSessionCookie(newToken).toString())
                    .body(Map.of(
                            "success", true,
                            "username", username.toLowerCase(),
                            "userId", userId.value().toString()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /auth/logout — Invalidates JWT and clears session cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token != null) {
            jwtValidator.validateFull(token).ifPresent(claims -> {
                if (claims.jti() != null) {
                    tokenBlacklist.revoke(claims.jti());
                    log.info("JWT revoked: jti={}, userId={}", claims.jti(), claims.userId());
                }
            });
        }

        ResponseCookie clearCookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true) // Required for SameSite=None
                .sameSite("None") // Required for cross-origin from ngrok to localhost
                .path("/")
                .maxAge(0) // expire immediately
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("success", true, "message", "Logged out successfully"));
    }

    /**
     * GET /auth/session — Returns session status for frontend.
     */
    @GetMapping("/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "userId", user.userId().toString(),
                "username", user.username() != null ? user.username() : "",
                "pendingRegistration", user.pendingRegistration()));
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * Idempotent agent provisioning.
     */
    private void provisionAgent(UserId userId) {
        if (agentPersistencePort.findByUserId(userId).isPresent()) {
            log.info("Agent already provisioned for userId={}", userId);
            return;
        }
        Agent agent = new Agent(new AgentId(UUID.randomUUID()), userId);
        agentPersistencePort.save(agent);
        log.info("Agent provisioned: agentId={} for userId={}", agent.getAgentId(), userId);
    }

    private ResponseCookie createSessionCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true) // Required for SameSite=None
                .sameSite("None") // Required for cross-origin from ngrok to localhost
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours
                .build();
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // ── DTOs ──────────────────────────────────────────────────

    public record UsernameRequest(
            String username,
            String email,
            String slackUserId,
            String workspaceId) {
    }
}
