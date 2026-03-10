package com.coagent4u.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.approval.port.in.DecideApprovalUseCase;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.security.SlackSignatureVerifier;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Handles Slack interactive component payloads (button clicks).
 *
 * <p>
 * Critical contract: Slack requires HTTP 200 within 3 seconds.
 * This handler responds IMMEDIATELY with an acknowledgment message,
 * then processes the action asynchronously on a thread pool.
 * </p>
 */
@RestController
@RequestMapping("/slack")
public class SlackInteractionHandler {

    private static final Logger log = LoggerFactory.getLogger(SlackInteractionHandler.class);

    private final SlackSignatureVerifier signatureVerifier;
    private final DecideApprovalUseCase decideApprovalUseCase;
    private final CoordinationProtocolPort coordinationProtocol;
    private final UserPersistencePort userPersistencePort;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;

    public SlackInteractionHandler(
            SlackSignatureVerifier signatureVerifier,
            DecideApprovalUseCase decideApprovalUseCase,
            CoordinationProtocolPort coordinationProtocol,
            UserPersistencePort userPersistencePort,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") Executor taskExecutor) {
        this.signatureVerifier = signatureVerifier;
        this.decideApprovalUseCase = decideApprovalUseCase;
        this.coordinationProtocol = coordinationProtocol;
        this.userPersistencePort = userPersistencePort;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Handles interactive payloads from Slack (button clicks).
     * Responds IMMEDIATELY with acknowledgment — all processing happens on
     * background thread.
     */
    @PostMapping(value = "/interactions", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> handleInteraction(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            HttpServletRequest request) throws IOException {

        // 1. Read raw body for signature verification
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        log.info("[InteractionHandler] Received Slack interaction, verifying signature...");

        if (!signatureVerifier.verify(timestamp, rawBody, signature)) {
            log.warn("[InteractionHandler] Signature verification failed");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        log.info("[InteractionHandler] Signature verified");

        try {
            // 2. Parse payload — Slack sends form-encoded: payload={json}
            String payloadJson = rawBody;
            if (rawBody.startsWith("payload=")) {
                payloadJson = java.net.URLDecoder.decode(
                        rawBody.substring("payload=".length()), "UTF-8");
            }

            JsonNode payload = objectMapper.readTree(payloadJson);
            String type = payload.path("type").asText();

            if (!"block_actions".equals(type)) {
                return ResponseEntity.ok("");
            }

            // 3. Extract action details
            JsonNode actions = payload.path("actions");
            if (actions.isEmpty()) {
                return ResponseEntity.ok("");
            }

            JsonNode action = actions.get(0);
            String actionId = action.path("action_id").asText();
            String actionValue = action.path("value").asText();

            // 4. Resolve Slack user
            String slackUserId = payload.path("user").path("id").asText();
            String teamId = payload.path("team").path("id").asText();

            log.info("[InteractionHandler] action_id={} value={} user={}", actionId, actionValue, slackUserId);

            // 5. Route action — respond IMMEDIATELY, process on background thread
            String responseText;

            if (actionId.startsWith("slot_select_")) {
                responseText = "🕐 Slot selected! Processing...";
                // Fire-and-forget on thread pool
                taskExecutor.execute(() -> processSlotSelection(actionId, actionValue, slackUserId, teamId));

            } else if ("approve_action".equals(actionId)) {
                responseText = "✅ Approved! Processing your request...";
                taskExecutor.execute(() -> processDecision(slackUserId, teamId, actionId, actionValue));

            } else if ("reject_action".equals(actionId)) {
                responseText = "❌ Rejected.";
                taskExecutor.execute(() -> processDecision(slackUserId, teamId, actionId, actionValue));

            } else {
                log.warn("[InteractionHandler] Unknown action_id={}", actionId);
                return ResponseEntity.ok("");
            }

            // 6. Return immediately — Slack gets response within milliseconds
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildResponseMessage(responseText));

        } catch (Exception e) {
            log.warn("[InteractionHandler] Error handling Slack interaction: {}", e.getMessage());
            return ResponseEntity.ok(buildResponseMessage("⚠️ An error occurred processing your response."));
        }
    }

    // ── Background Slot Selection ──

    /**
     * Processes slot selection on a background thread.
     * action_id format: slot_select_{coordinationId}_{slotIndex}
     * value format: {startEpochMs}_{endEpochMs}
     */
    private void processSlotSelection(String actionId, String value, String slackUserId, String teamId) {
        try {
            // Parse coordination ID from action_id
            String withoutPrefix = actionId.substring("slot_select_".length());
            int lastUnderscore = withoutPrefix.lastIndexOf('_');
            String coordIdStr = withoutPrefix.substring(0, lastUnderscore);

            // Parse slot from value: startMs_endMs
            String[] parts = value.split("_");
            Instant start = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            Instant end = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            TimeSlot selectedSlot = new TimeSlot(start, end);

            CoordinationId coordId = new CoordinationId(UUID.fromString(coordIdStr));

            log.info("[InteractionHandler] Slot selected: coordination={} slot={}-{}", coordId, start, end);

            coordinationProtocol.selectSlot(coordId, selectedSlot);

            log.info("[InteractionHandler] Slot selection processed for coordination={}", coordId);

        } catch (Exception e) {
            log.warn("[InteractionHandler] Failed to process slot selection: {}", e.getMessage());
        }
    }

    // ── Background Approval Decision ──

    /**
     * Processes approval decision on a background thread.
     */
    private void processDecision(String slackUserId, String teamId, String actionId, String approvalIdStr) {
        try {
            Optional<User> userOpt = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackUserId), new WorkspaceId(teamId));

            if (userOpt.isEmpty()) {
                log.warn("[InteractionHandler] No registered user for Slack interaction from user={}", slackUserId);
                return;
            }

            com.coagent4u.shared.UserId userId = userOpt.get().getUserId();
            ApprovalId approvalId = new ApprovalId(UUID.fromString(approvalIdStr));

            ApprovalStatus decision = "approve_action".equals(actionId)
                    ? ApprovalStatus.APPROVED
                    : ApprovalStatus.REJECTED;

            decideApprovalUseCase.decide(approvalId, userId, decision);
            log.info("[InteractionHandler] Approval {} {} by user={}", approvalIdStr, decision, slackUserId);

        } catch (Exception e) {
            log.warn("[InteractionHandler] Failed to process approval decision: {}", e.getMessage());
        }
    }

    /**
     * Builds a JSON response that replaces the original message with a status
     * update.
     */
    private String buildResponseMessage(String text) {
        try {
            var root = objectMapper.createObjectNode();
            root.put("response_type", "in_channel");
            root.put("replace_original", true);
            root.put("text", text);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"text\":\"" + text + "\"}";
        }
    }
}
