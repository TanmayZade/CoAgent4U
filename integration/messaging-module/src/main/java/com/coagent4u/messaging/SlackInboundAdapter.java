package com.coagent4u.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
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
 * This adapter immediately acknowledges and processes async via
 * a dedicated thread pool (not Spring @Async self-invocation).
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
    private final Executor taskExecutor;

    // Bounded in-memory dedup cache: event_id → timestamp
    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();

    public SlackInboundAdapter(
            SlackSignatureVerifier signatureVerifier,
            UserPersistencePort userPersistencePort,
            AgentPersistencePort agentPersistencePort,
            HandleMessageUseCase handleMessageUseCase,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") Executor taskExecutor) {
        this.signatureVerifier = signatureVerifier;
        this.userPersistencePort = userPersistencePort;
        this.agentPersistencePort = agentPersistencePort;
        this.handleMessageUseCase = handleMessageUseCase;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Handles all Slack Events API webhooks.
     * - URL verification challenge: responds synchronously
     * - Event callbacks: acknowledges IMMEDIATELY, processes on a separate thread
     */
    @PostMapping("/events")
    public ResponseEntity<String> handleEvent(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            @RequestBody String rawBody) {

        // 1. Verify Slack signature (replay protection)
        if (!signatureVerifier.verify(timestamp, rawBody, signature)) {
            log.warn("[SlackAdapter] Signature verification failed");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String type = payload.path("type").asText();

            // 2. URL Verification challenge (Slack app setup)
            if ("url_verification".equals(type)) {
                String challenge = payload.path("challenge").asText();
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain")
                        .body(challenge);
            }

            // 3. Event callback — acknowledge IMMEDIATELY, process async
            if ("event_callback".equals(type)) {
                String eventId = payload.path("event_id").asText();

                // Idempotency guard
                if (isDuplicate(eventId)) {
                    return ResponseEntity.ok("");
                }

                JsonNode event = payload.path("event");
                String eventType = event.path("type").asText();
                String subtype = event.path("subtype").asText(null);

                // Skip non-message events we don't care about
                if (!"message".equals(eventType) && !"app_mention".equals(eventType)) {
                    return ResponseEntity.ok("");
                }

                // Skip bot messages, message updates (changed), and deleted messages
                if ("bot_message".equals(subtype) || "message_changed".equals(subtype) 
                        || "message_deleted".equals(subtype) || event.has("bot_id")) {
                    return ResponseEntity.ok("");
                }

                String slackUserId = event.path("user").asText();
                if (slackUserId == null || slackUserId.isBlank()) {
                    log.debug("[SlackAdapter] Skipping event_id={} because user field is blank (often for system messages)", eventId);
                    return ResponseEntity.ok("");
                }

                String teamId = payload.path("team_id").asText();
                String text = event.path("text").asText();

                if (text == null || text.isBlank()) {
                    return ResponseEntity.ok("");
                }

                // Preserve user mentions as slack:USER_ID for intent parsing
                    // but strip the bot self-mention (for app_mention events)
                    java.util.regex.Matcher mentionMatcher = java.util.regex.Pattern
                            .compile("<@([A-Z0-9]+)>").matcher(text);
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    while (mentionMatcher.find()) {
                        String mentionedUserId = mentionMatcher.group(1);
                        if (first && "app_mention".equals(eventType)) {
                            mentionMatcher.appendReplacement(sb, "");
                            first = false;
                        } else {
                            mentionMatcher.appendReplacement(sb, "slack:" + mentionedUserId);
                        }
                    }
                    mentionMatcher.appendTail(sb);
                    text = sb.toString().trim();


                    // Fire-and-forget on thread pool (NOT @Async self-invocation)
                    final String finalText = text;
                    taskExecutor.execute(() -> processMessage(slackUserId, teamId, finalText, eventId));


                // Return OK immediately — processing continues on background thread
                return ResponseEntity.ok("");
            }

            return ResponseEntity.ok("");

        } catch (Exception e) {
            log.warn("[SlackAdapter] Error parsing event payload: {}", e.getMessage());
            return ResponseEntity.ok("");
        }
    }

    /**
     * Background processing — runs on a separate thread, outside the Slack
     * deadline.
     */
    private void processMessage(String slackUserId, String teamId, String text, String eventId) {
        try {
            log.info("[SlackAdapter] Processing event_id={} from user={}", eventId, slackUserId);

            // Resolve Slack user → domain User → Agent
            Optional<User> userOpt = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackUserId), new WorkspaceId(teamId));

            if (userOpt.isEmpty()) {
                log.warn("[SlackAdapter] No registered user for slackUserId={}", slackUserId);
                return;
            }

            User user = userOpt.get();
            UserId userId = user.getUserId();

            var agentOpt = agentPersistencePort.findByUserId(userId);
            if (agentOpt.isEmpty()) {
                log.warn("[SlackAdapter] No agent for userId={}", userId);
                return;
            }

            AgentId agentId = agentOpt.get().getAgentId();

            // Delegate to domain use case
            handleMessageUseCase.handleMessage(agentId, text);

        } catch (Exception e) {
            // Catch ALL exceptions — no stack traces in terminal
            log.warn("[SlackAdapter] Failed to process event_id={}: {}", eventId, e.getMessage());
        }
    }

    /**
     * Checks if event was already processed. Evicts stale entries.
     */
    private boolean isDuplicate(String eventId) {
        Instant now = Instant.now();
        processedEvents.entrySet().removeIf(
                entry -> java.time.Duration.between(entry.getValue(), now).getSeconds() > DEDUP_TTL_SECONDS);
        return processedEvents.putIfAbsent(eventId, now) != null;
    }
}
