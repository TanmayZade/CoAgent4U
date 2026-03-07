package com.coagent4u.app.rest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.domain.IntentParser;
import com.coagent4u.agent.domain.ParsedIntent;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.ConnectServiceUseCase;
import com.coagent4u.user.port.in.RegisterUserUseCase;
import com.coagent4u.user.port.out.OAuthTokenExchangePort;
import com.coagent4u.user.port.out.OAuthTokenExchangePort.OAuthTokenResult;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * REST API controller for the CoAgent4U web dashboard.
 * Exposes endpoints for user management, schedule viewing, and OAuth callbacks.
 */
@RestController
@RequestMapping("/api")
public class RestApiController {

    private static final Logger log = LoggerFactory.getLogger(RestApiController.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final ConnectServiceUseCase connectServiceUseCase;
    private final UserPersistencePort userPersistencePort;
    private final OAuthTokenExchangePort oAuthTokenExchangePort;
    private final LLMPort llmPort;
    private final CoagentProperties coagentProperties;
    private final AgentPersistencePort agentPersistencePort;
    private final IntentParser intentParser = new IntentParser();

    public RestApiController(
            RegisterUserUseCase registerUserUseCase,
            ConnectServiceUseCase connectServiceUseCase,
            UserPersistencePort userPersistencePort,
            OAuthTokenExchangePort oAuthTokenExchangePort,
            LLMPort llmPort,
            CoagentProperties coagentProperties,
            AgentPersistencePort agentPersistencePort) {
        this.registerUserUseCase = registerUserUseCase;
        this.connectServiceUseCase = connectServiceUseCase;
        this.userPersistencePort = userPersistencePort;
        this.oAuthTokenExchangePort = oAuthTokenExchangePort;
        this.llmPort = llmPort;
        this.coagentProperties = coagentProperties;
        this.agentPersistencePort = agentPersistencePort;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/users")
    public ResponseEntity<String> registerUser(@RequestBody RegisterUserRequest request) {
        try {
            registerUserUseCase.register(
                    new UserId(java.util.UUID.randomUUID()),
                    request.username(),
                    new Email(request.email()),
                    new SlackUserId(request.slackUserId()),
                    new WorkspaceId(request.workspaceId()));

            return ResponseEntity.ok("User registered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Provision an agent for a registered user (local testing helper).
     * Pass the userId returned by GET /api/users/{userId} or from logs.
     */
    @PostMapping("/agents")
    public ResponseEntity<String> provisionAgent(@RequestBody ProvisionAgentRequest request) {
        try {
            UserId userId = new UserId(java.util.UUID.fromString(request.userId()));
            if (userPersistencePort.findById(userId).isEmpty()) {
                return ResponseEntity.badRequest().body("User not found: " + request.userId());
            }
            // Idempotent: skip if agent already exists
            if (agentPersistencePort.findByUserId(userId).isPresent()) {
                return ResponseEntity.ok("Agent already provisioned for user: " + request.userId());
            }
            Agent agent = new Agent(new AgentId(java.util.UUID.randomUUID()), userId);
            agentPersistencePort.save(agent);
            log.info("Agent provisioned: agentId={} for userId={}", agent.getAgentId(), userId);
            return ResponseEntity.ok("Agent provisioned: " + agent.getAgentId().value());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid userId format: " + e.getMessage());
        }
    }

    /**
     * Get user profile.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        Optional<User> user = userPersistencePort.findById(new UserId(java.util.UUID.fromString(userId)));
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User u = user.get();
        return ResponseEntity.ok(new UserProfileResponse(
                u.getUserId().value().toString(),
                u.getUsername(),
                u.getEmail().value(),
                u.isDeleted()));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Redirect user to Google OAuth consent screen.
     * The userId is embedded in the 'state' parameter so we know which user
     * initiated the flow when the callback arrives.
     */
    @GetMapping("/oauth2/authorize")
    public ResponseEntity<Void> oauthAuthorize(@RequestParam String userId) {
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + coagentProperties.getGoogle().getClientId()
                + "&redirect_uri=" + coagentProperties.getGoogle().getRedirectUri()
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/calendar.events"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + userId;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    /**
     * Google OAuth2 callback — exchanges authorization code for tokens,
     * encrypts them, and stores them via ConnectServiceUseCase.
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<String> oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state) {
        if (error != null) {
            log.warn("OAuth2 callback error: {}", error);
            return ResponseEntity.badRequest().body("OAuth error: " + error);
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing authorization code");
        }
        if (state == null || state.isBlank()) {
            return ResponseEntity.badRequest().body("Missing state (userId)");
        }

        try {
            UserId userId = new UserId(java.util.UUID.fromString(state));

            // 1. Exchange code for encrypted tokens
            OAuthTokenResult tokens = oAuthTokenExchangePort.exchangeCode(code);

            // 2. Store encrypted tokens via ConnectServiceUseCase
            connectServiceUseCase.connect(
                    userId,
                    "GOOGLE_CALENDAR",
                    tokens.encryptedAccessToken(),
                    tokens.encryptedRefreshToken(),
                    tokens.expiresAt());

            log.info("Google Calendar connected for user={}", userId);
            return ResponseEntity.ok("Google Calendar connected successfully! You can close this window.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid user ID in state parameter: " + e.getMessage());
        } catch (Exception e) {
            log.error("OAuth2 callback failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to connect Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Sandbox: Test intent parsing logic (Tier 1 regex + Tier 2 LLM).
     */
    @PostMapping("/sandbox/parse-intent")
    public ResponseEntity<ParsedIntentSandboxResponse> parseIntent(@RequestBody SandboxParseRequest request) {
        // Tier 1: Local Regex
        ParsedIntent tier1 = intentParser.parse(request.text());

        // Tier 2: LLM Fallback (only if Tier 1 is UNKNOWN, or user forced it)
        String llmIntent = "SKIPPED";
        String debugStatus = "Tier 1 matched";

        if (tier1.type().name().equals("UNKNOWN") || request.forceLlm()) {
            AgentId dummyId = new AgentId(java.util.UUID.randomUUID());
            try {
                Optional<String> result = llmPort.classifyIntent(dummyId, request.text());
                if (result.isPresent()) {
                    llmIntent = result.get();
                    debugStatus = "Success";
                } else {
                    llmIntent = "FAILED_OR_DISABLED";
                    debugStatus = String.format("LLM returned empty. Enabled: %s, Key: %s..., Model: %s",
                            coagentProperties.getLlm().isEnabled(),
                            coagentProperties.getLlm().getGroqApiKey() != null
                                    ? coagentProperties.getLlm().getGroqApiKey().substring(0,
                                            Math.min(7, coagentProperties.getLlm().getGroqApiKey().length()))
                                    : "null",
                            coagentProperties.getLlm().getModel());
                }
            } catch (Exception e) {
                llmIntent = "ERROR";
                debugStatus = "Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage();
            }
        }

        return ResponseEntity.ok(new ParsedIntentSandboxResponse(
                tier1.type().name(),
                tier1.params(),
                llmIntent,
                tier1.type().name().equals("UNKNOWN") ? llmIntent : tier1.type().name(),
                debugStatus));
    }

    // ── DTOs ──────────────────────────────────────────────────

    public record SandboxParseRequest(String text, boolean forceLlm) {
    }

    public record ParsedIntentSandboxResponse(
            String tier1Type,
            java.util.Map<String, String> tier1Params,
            String tier2Type,
            String finalDecision,
            String debugStatus) {
    }

    public record RegisterUserRequest(
            String username,
            String email,
            String slackUserId,
            String workspaceId) {
    }

    public record ProvisionAgentRequest(String userId) {
    }

    public record UserProfileResponse(
            String userId,
            String username,
            String email,
            boolean deleted) {
    }
}
