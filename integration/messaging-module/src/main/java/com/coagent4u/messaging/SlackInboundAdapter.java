package com.coagent4u.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.agent.port.in.HandleMessageUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.security.SlackSignatureVerifier;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Slack Events API webhook handler.
 *
 * <p>
 * Critical contract: Slack requires HTTP 200 within 3 seconds.
 * This adapter immediately acknowledges and processes async.
 * </p>
 *
 * <p>
 * Idempotency: Deduplicates by Slack event_id using a bounded in-memory cache.
 * </p>
 */
@RestController
@RequestMapping("/slack")
public class SlackInboundAdapter {

    private static final Logger log = LoggerFactory.getLogger(SlackInboundAdapter.class);
    private static final long DEDUP_TTL_SECONDS = 600; // 10 minutes

    private final SlackSignatureVerifier signatureVerifier;
    private final UserPersistencePort userPersistencePort;
    private final AgentPersistencePort agentPersistencePort;
    private final HandleMessageUseCase handleMessageUseCase;
    private final ObjectMapper objectMapper;

    // Bounded in-memory dedup cache: event_id → timestamp
    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();

    public SlackInboundAdapter(
            SlackSignatureVerifier signatureVerifier,
            UserPersistencePort userPersistencePort,
            AgentPersistencePort agentPersistencePort,
            HandleMessageUseCase handleMessageUseCase,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.userPersistencePort = userPersistencePort;
        this.agentPersistencePort = agentPersistencePort;
        this.handleMessageUseCase = handleMessageUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles all Slack Events API webhooks.
     * - URL verification challenge: responds synchronously
     * - Event callbacks: acknowledges immediately, processes async
     */
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestBody String rawBody) {

        // log.info("Received Slack event request, verifying signature...");

        // 1. Verify Slack signature (replay protection)
        if (!signatureVerifier.verify(timestamp, rawBody, signature)) {
            log.warn("Slack signature verification failed - timestamp={}, signature={}", timestamp, signature);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // log.info("Slack signature verified successfully");

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String type = payload.path("type").asText();

            // 2. URL Verification challenge (Slack app setup)
            if ("url_verification".equals(type)) {
                String challenge = payload.path("challenge").asText();
                // log.info("Slack URL verification challenge received, responding with
                // challenge");
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain")
                        .body(challenge);
            }

            // 3. Event callback — acknowledge immediately, process async
            if ("event_callback".equals(type)) {
                String eventId = payload.path("event_id").asText();

                // Idempotency guard
                if (isDuplicate(eventId)) {
                    log.debug("Duplicate event_id={}, skipping", eventId);
                    return ResponseEntity.ok("");
                }

                JsonNode event = payload.path("event");
                String eventType = event.path("type").asText();

                if ("message".equals(eventType) || "app_mention".equals(eventType)) {
                    // Skip bot messages (our own replies echo back through Slack)
                    String subtype = event.path("subtype").asText(null);
                    if ("bot_message".equals(subtype) || event.has("bot_id")) {
                        log.debug("Ignoring bot message event_id={}", eventId);
                        return ResponseEntity.ok("");
                    }

                    String slackUserId = event.path("user").asText();
                    String teamId = payload.path("team_id").asText();
                    String text = event.path("text").asText();

                    // Strip Slack mention prefix like "<@U0BOTID> " so intent parsing works
                    text = text.replaceAll("<@[A-Z0-9]+>\\s*", "").trim();

                    // Fire-and-forget async processing
                    processMessageAsync(slackUserId, teamId, text, eventId);
                }

                return ResponseEntity.ok("");
            }

            return ResponseEntity.ok("");

        } catch (Exception e) {
            log.error("Error parsing Slack event payload", e);
            // Still return 200 to prevent Slack retries
            return ResponseEntity.ok("");
        }
    }

    /**
     * Async processing — runs outside the 3-second Slack deadline.
     */
    @Async
    public void processMessageAsync(String slackUserId, String teamId, String text, String eventId) {
        try {
            log.info("Processing Slack message event_id={} from user={}", eventId, slackUserId);

            // Resolve Slack user → domain User → Agent
            Optional<User> userOpt = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackUserId), new WorkspaceId(teamId));

            if (userOpt.isEmpty()) {
                log.warn("No registered user for slackUserId={}, teamId={}", slackUserId, teamId);
                return;
            }

            User user = userOpt.get();
            UserId userId = user.getUserId();

            var agentOpt = agentPersistencePort.findByUserId(userId);
            if (agentOpt.isEmpty()) {
                log.warn("No agent provisioned for userId={}", userId);
                return;
            }

            AgentId agentId = agentOpt.get().getAgentId();

            // Delegate to domain use case
            handleMessageUseCase.handleMessage(agentId, text);

        } catch (Exception e) {
            log.error("Failed to process Slack message event_id={}: {}", eventId, e.getMessage(), e);
        }
    }

    /**
     * Checks if event was already processed. Evicts stale entries.
     */
    private boolean isDuplicate(String eventId) {
        Instant now = Instant.now();

        // Evict stale entries (older than TTL)
        processedEvents.entrySet().removeIf(
                entry -> java.time.Duration.between(entry.getValue(), now).getSeconds() > DEDUP_TTL_SECONDS);

        // Check and mark
        return processedEvents.putIfAbsent(eventId, now) != null;
    }
}
