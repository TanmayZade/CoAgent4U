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
import com.coagent4u.user.domain.WorkspaceInstallation;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.WorkspaceInstallationPersistencePort;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication controller for Slack OAuth login, username registration,
 * logout, and session verification.
 *
 * <p>
 * Frontend uses {@code username}; backend uses {@code userId} internally.
 * Slack identity always uses slackUserId + workspaceId (teamId).
 * </p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String COOKIE_NAME = "coagent_session";
    private static final java.util.regex.Pattern USERNAME_PATTERN = java.util.regex.Pattern
            .compile("^[a-zA-Z0-9_-]{3,32}$");

    private final CoagentProperties properties;
    private final SlackOAuthPort slackOAuthPort;
    private final UserPersistencePort userPersistencePort;
    private final WorkspaceInstallationPersistencePort workspaceInstallationPersistencePort;
    private final RegisterUserUseCase registerUserUseCase;
    private final AgentPersistencePort agentPersistencePort;
    private final NotificationPort notificationPort;
    private final JwtIssuer jwtIssuer;
    private final JwtValidator jwtValidator;
    private final JwtTokenBlacklist tokenBlacklist;

    public AuthController(
            CoagentProperties properties,
            SlackOAuthPort slackOAuthPort,
            UserPersistencePort userPersistencePort,
            WorkspaceInstallationPersistencePort workspaceInstallationPersistencePort,
            RegisterUserUseCase registerUserUseCase,
            AgentPersistencePort agentPersistencePort,
            NotificationPort notificationPort,
            JwtIssuer jwtIssuer,
            JwtValidator jwtValidator,
            JwtTokenBlacklist tokenBlacklist) {
        this.properties = properties;
        this.slackOAuthPort = slackOAuthPort;
        this.userPersistencePort = userPersistencePort;
        this.workspaceInstallationPersistencePort = workspaceInstallationPersistencePort;
        this.registerUserUseCase = registerUserUseCase;
        this.agentPersistencePort = agentPersistencePort;
        this.notificationPort = notificationPort;
        this.jwtIssuer = jwtIssuer;
        this.jwtValidator = jwtValidator;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * GET /auth/slack/start — Redirects to Slack OAuth consent screen.
     * Uses OpenID Connect scopes: openid, profile, email, users:read,
     * users:read.email.
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
     * GET /auth/slack/install/start — Redirects to Slack App installation screen.
     * Requests bot scopes for messaging and interaction.
     */
    @GetMapping("/slack/install/start")
    public ResponseEntity<Void> slackInstallStart() {
        String authUrl = "https://slack.com/oauth/v2/authorize"
                + "?client_id=" + properties.getSlack().getClientId()
                + "&scope=chat:write,chat:write.public,commands"
                + "&user_scope=identity.basic,identity.email,identity.avatar"
                + "&redirect_uri=" + properties.getSlack().getRedirectUri().replace("/callback", "/install/callback");

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    /**
     * GET /auth/slack/callback — Handles Slack OAuth callback.
     * <ol>
     * <li>Exchanges code for Slack tokens + user identity</li>
     * <li>Looks up user by slackUserId + workspaceId</li>
     * <li>Existing user → Issue JWT, redirect to frontend</li>
     * <li>New user → Issue pending JWT, redirect to onboarding page</li>
     * </ol>
     */
    @GetMapping("/slack/callback")
    public ResponseEntity<Void> slackCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            @RequestParam Map<String, String> allParams) {

        log.info("Slack OAuth callback received: code={}, error={}, state={}, allParams={}",
                code != null ? "[PRESENT]" : "[NULL]", error, state, allParams);

        if (error != null) {
            log.warn("Slack OAuth error: {}", error);
            String redirectUrl = properties.getFrontendUrl() + "/signin?error=" + error;
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION,
                            properties.getFrontendUrl() + "/signin?error=missing_code")
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
                        false,
                        slackResult.slackUserId(),
                        slackResult.workspaceId(),
                        slackResult.workspaceName(),
                        slackResult.workspaceDomain(),
                        slackResult.displayName(),
                        slackResult.avatarUrl());

                log.info("Slack login successful for existing user={}", user.getUsername());

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, properties.getFrontendUrl() + "/dashboard")
                        .header(HttpHeaders.SET_COOKIE, createSessionCookie(token).toString())
                        .build();
            } else {
                // New user — issue pending registration JWT with Slack identity embedded
                UUID tempUserId = UUID.randomUUID();
                String token = jwtIssuer.issuePending(tempUserId,
                        slackResult.slackUserId(),
                        slackResult.workspaceId(),
                        slackResult.workspaceName(),
                        slackResult.workspaceDomain(),
                        slackResult.email(),
                        slackResult.displayName(),
                        slackResult.avatarUrl());

                log.info("New Slack user detected: slackUserId={}, workspaceId={}",
                        slackResult.slackUserId(), slackResult.workspaceId());

                // Redirect to onboarding — no Slack identity in URL (it's in the signed JWT)
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION,
                                properties.getFrontendUrl() + "/onboarding")
                        .header(HttpHeaders.SET_COOKIE, createSessionCookie(token).toString())
                        .build();
            }

        } catch (Exception e) {
            log.error("Slack OAuth callback failed", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION,
                            properties.getFrontendUrl() + "/signin?error=oauth_failed")
                    .build();
        }
    }

    /**
     * POST /auth/username — Completes onboarding for new users.
     * <ol>
     * <li>Validates pending registration state from JWT</li>
     * <li>Extracts Slack identity from JWT (server-side trusted)</li>
     * <li>Validates username format: {@code ^[a-zA-Z0-9_-]{3,32}$}</li>
     * <li>Creates User via RegisterUserUseCase</li>
     * <li>Provisions Agent (idempotent) in service layer</li>
     * <li>Reissues JWT with full claims</li>
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

        // Guard: must be pending registration
        if (!authUser.pendingRegistration()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "User already registered"));
        }

        // Guard: must have Slack identity from JWT (server-side trusted)
        if (authUser.slackUserId() == null || authUser.workspaceId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing Slack identity in session. Please restart login."));
        }

        // Validate username
        String username = request.username();
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username must match ^[a-zA-Z0-9_-]{3,32}$"));
        }

        try {
            // Create user via use case — Slack identity from JWT (trusted)
            UserId userId = new UserId(UUID.randomUUID());
            Email email = (authUser.email() != null && !authUser.email().isBlank())
                    ? new Email(authUser.email())
                    : new Email(username + "@coagent4u.local");

            registerUserUseCase.register(
                    userId,
                    username.toLowerCase(),
                    email,
                    new SlackUserId(authUser.slackUserId()),
                    new WorkspaceId(authUser.workspaceId()),
                    authUser.workspaceName(),
                    authUser.workspaceDomain(),
                    authUser.email(),
                    authUser.displayName(),
                    authUser.avatarUrl());

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
            String newToken = jwtIssuer.issue(userId.value(), username.toLowerCase(), false,
                    authUser.slackUserId(), authUser.workspaceId(), authUser.workspaceName(),
                    authUser.workspaceDomain(), authUser.displayName(), authUser.avatarUrl());

            log.info("User registered: username={}, userId={}", username, userId);

            // Send welcome message if workspace has app installed
            WorkspaceId workspaceId = new WorkspaceId(authUser.workspaceId());
            if (workspaceInstallationPersistencePort.findByWorkspaceId(workspaceId).isPresent()) {
                sendWelcomeMessage(new SlackUserId(authUser.slackUserId()), workspaceId);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, createSessionCookie(newToken).toString())
                    .body(Map.of(
                            "success", true,
                            "username", username.toLowerCase()));

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
                .secure(true)
                .sameSite("None")
                .domain(".coagent4u.com")
                .path("/")
                .maxAge(0) // expire immediately
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "pendingRegistration", user.pendingRegistration()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isSlackAppInstalled = false;
        if (user.workspaceId() != null) {
            isSlackAppInstalled = workspaceInstallationPersistencePort
                    .findByWorkspaceId(new WorkspaceId(user.workspaceId()))
                    .map(WorkspaceInstallation::active)
                    .orElse(false);
        }

        return ResponseEntity.ok(Map.of(
                "username", user.username() != null ? user.username() : "",
                "pendingRegistration", user.pendingRegistration(),
                "slack_name", user.displayName() != null ? user.displayName() : "",
                "slack_workspace", user.workspaceName() != null ? user.workspaceName() : "",
                "slack_workspace_domain", user.workspaceDomain() != null ? user.workspaceDomain() : "",
                "slack_email", user.email() != null ? user.email() : "",
                "slack_avatar_url", user.avatarUrl() != null ? user.avatarUrl() : "",
                "isSlackAppInstalled", isSlackAppInstalled));
    }

    /**
     * GET /auth/slack/install/callback — Handles bot installation callback.
     * Exchanges code for bot token and stores it in workspace_installations.
     */
    @GetMapping("/slack/install/callback")
    public ResponseEntity<Void> slackInstallCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        log.info("Slack App Install callback: code={}, error={}",
                code != null ? "[PRESENT]" : "[NULL]", error);

        if (error != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, properties.getFrontendUrl() + "/dashboard?error=" + error)
                    .build();
        }

        try {
            SlackOAuthPort.SlackInstallationResult result = slackOAuthPort.exchangeForBotToken(code);

            workspaceInstallationPersistencePort.save(new WorkspaceInstallation(
                    new WorkspaceId(result.workspaceId()),
                    result.botToken(),
                    result.installerUserId(),
                    java.time.Instant.now(),
                    true));

            log.info("CoAgent4U App successfully installed in workspace={}", result.workspaceId());

            // Send installation welcome message to installer
            sendWelcomeMessage(new SlackUserId(result.installerUserId()), new WorkspaceId(result.workspaceId()));

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, properties.getFrontendUrl() + "/dashboard?installed=true")
                    .build();

        } catch (Exception e) {
            log.error("Slack App Installation exchange failed", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, properties.getFrontendUrl() + "/dashboard?error=install_failed")
                    .build();
        }
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

    private void sendWelcomeMessage(SlackUserId slackUserId, WorkspaceId workspaceId) {
        String welcomeMessage = "Thank you for choosing CoAgent4U! I am your personal agent who can perform personalized tasks as well as collaborative tasks with other users' agents too. \n\n"
                + "You can interact with me directly in this DM using natural human language. Here are some things I can help you with:\n"
                + "1) View your schedule\n"
                + "2) Add an event\n"
                + "3) Coordinate a meeting with another user (e.g., 'coordinate meeting with @user_id')\n\n"
                + "How can I help you today?";

        try {
            notificationPort.sendMessage(slackUserId, workspaceId, welcomeMessage);
            log.info("Welcome message sent to slackUserId={} in workspaceId={}", slackUserId, workspaceId);
        } catch (Exception e) {
            log.error("Failed to send welcome message to slackUserId={} in workspaceId={}", slackUserId, workspaceId, e);
        }
    }

    private ResponseCookie createSessionCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true) // HTTPS only
                .sameSite("None") // Required: frontend=coagent4u.com, API=api.coagent4u.com
                .domain(".coagent4u.com") // Share cookie across subdomains
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours
                .build();
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // ── DTOs ──────────────────────────────────────────────────

    /**
     * Username request DTO — only accepts username.
     * Slack identity (slackUserId, workspaceId, email) is read from the
     * trusted pending JWT, NOT from the request body.
     */
    public record UsernameRequest(String username) {
    }
}
