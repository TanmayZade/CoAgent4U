package com.coagent4u.app.rest;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.agent.domain.IntentParser;
import com.coagent4u.agent.domain.ParsedIntent;
import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.RegisterUserUseCase;
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
    private final UserPersistencePort userPersistencePort;
    private final LLMPort llmPort;
    private final com.coagent4u.config.CoagentProperties coagentProperties;
    private final IntentParser intentParser = new IntentParser();

    public RestApiController(
            RegisterUserUseCase registerUserUseCase,
            UserPersistencePort userPersistencePort,
            LLMPort llmPort,
            com.coagent4u.config.CoagentProperties coagentProperties) {
        this.registerUserUseCase = registerUserUseCase;
        this.userPersistencePort = userPersistencePort;
        this.llmPort = llmPort;
        this.coagentProperties = coagentProperties;
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
     * Google OAuth2 callback.
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<String> oauthCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        if (error != null) {
            log.warn("OAuth2 callback error: {}", error);
            return ResponseEntity.badRequest().body("OAuth error: " + error);
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Missing authorization code");
        }
        // TODO: Phase 4 — exchange code for tokens and store encrypted
        log.info("OAuth2 callback received with code (exchange not yet implemented)");
        return ResponseEntity.ok("OAuth callback received. Token exchange will be implemented in Phase 4.");
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

    public record UserProfileResponse(
            String userId,
            String username,
            String email,
            boolean deleted) {
    }
}
