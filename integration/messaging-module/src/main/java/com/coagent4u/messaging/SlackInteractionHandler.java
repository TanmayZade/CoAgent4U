package com.coagent4u.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.approval.port.in.DecideApprovalUseCase;
import com.coagent4u.security.SlackSignatureVerifier;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.SlackUserId;
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
 * Receives POST /slack/interactions when a user clicks [Approve] or [Reject]
 * on an approval request message. Verifies the Slack signature, resolves
 * the Slack user to a domain user, and calls {@link DecideApprovalUseCase}.
 * </p>
 */
@RestController
@RequestMapping("/slack")
public class SlackInteractionHandler {

    private static final Logger log = LoggerFactory.getLogger(SlackInteractionHandler.class);

    private final SlackSignatureVerifier signatureVerifier;
    private final DecideApprovalUseCase decideApprovalUseCase;
    private final UserPersistencePort userPersistencePort;
    private final ObjectMapper objectMapper;

    public SlackInteractionHandler(
            SlackSignatureVerifier signatureVerifier,
            DecideApprovalUseCase decideApprovalUseCase,
            UserPersistencePort userPersistencePort,
            ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.decideApprovalUseCase = decideApprovalUseCase;
        this.userPersistencePort = userPersistencePort;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles interactive payloads from Slack (button clicks on approval messages).
     * Returns immediately with a response message (within Slack's 3-second window)
     * and processes the approval decision asynchronously.
     */
    @PostMapping(value = "/interactions", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> handleInteraction(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            HttpServletRequest request) throws IOException {

        // 1. Read the raw body for signature verification
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        log.info("Received Slack interaction, verifying signature...");

        if (!signatureVerifier.verify(timestamp, rawBody, signature)) {
            log.warn("Slack interaction signature verification failed");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        log.info("Slack interaction signature verified successfully");

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
            String approvalIdStr = action.path("value").asText();

            // 4. Resolve Slack user → domain user
            String slackUserId = payload.path("user").path("id").asText();
            String teamId = payload.path("team").path("id").asText();

            // 5. Determine response text immediately
            String responseText;
            if ("approve_action".equals(actionId)) {
                responseText = "✅ Approved! Processing your request...";
            } else if ("reject_action".equals(actionId)) {
                responseText = "❌ Rejected.";
            } else {
                log.warn("Unknown action_id={}", actionId);
                return ResponseEntity.ok("");
            }

            // 6. Fire-and-forget: process the decision asynchronously
            processDecisionAsync(slackUserId, teamId, actionId, approvalIdStr);

            // 7. Respond immediately (within 3 seconds) — replaces the buttons
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildResponseMessage(responseText));

        } catch (Exception e) {
            log.error("Error handling Slack interaction: {}", e.getMessage(), e);
            return ResponseEntity.ok(buildResponseMessage("⚠️ An error occurred processing your response."));
        }
    }

    /**
     * Processes the approval decision asynchronously (outside the 3-second Slack
     * window).
     */
    @Async
    public void processDecisionAsync(String slackUserId, String teamId, String actionId, String approvalIdStr) {
        try {
            Optional<User> userOpt = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackUserId), new WorkspaceId(teamId));

            if (userOpt.isEmpty()) {
                log.warn("No registered user for Slack interaction from user={}", slackUserId);
                return;
            }

            com.coagent4u.shared.UserId userId = userOpt.get().getUserId();
            ApprovalId approvalId = new ApprovalId(java.util.UUID.fromString(approvalIdStr));

            ApprovalStatus decision = "approve_action".equals(actionId)
                    ? ApprovalStatus.APPROVED
                    : ApprovalStatus.REJECTED;

            decideApprovalUseCase.decide(approvalId, userId, decision);
            log.info("Approval {} {} by user={}", approvalIdStr, decision, slackUserId);

        } catch (Exception e) {
            log.error("Failed to process approval decision: {}", e.getMessage(), e);
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
