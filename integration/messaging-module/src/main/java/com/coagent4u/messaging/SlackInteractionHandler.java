package com.coagent4u.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.approval.domain.ApprovalStatus;
import com.coagent4u.approval.port.in.DecideApprovalUseCase;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.security.SlackSignatureVerifier;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.ApprovalId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Handles Slack interactive component payloads (button clicks).
 *
 * <p>
 * Critical contract: Slack requires HTTP 200 within 3 seconds.
 * This handler responds IMMEDIATELY, then processes the action
 * and performs delete-and-repost asynchronously.
 * </p>
 */
@RestController
@RequestMapping("/slack")
public class SlackInteractionHandler {

    private static final Logger log = LoggerFactory.getLogger(SlackInteractionHandler.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy 'at' h:mm a");

    // Idempotency guard — prevents double-processing of rapid clicks
    private final ConcurrentHashMap<String, Boolean> processedInteractions = new ConcurrentHashMap<>();
    private static final int MAX_PROCESSED_CACHE = 500;

    private final SlackSignatureVerifier signatureVerifier;
    private final DecideApprovalUseCase decideApprovalUseCase;
    private final CoordinationProtocolPort coordinationProtocol;
    private final UserPersistencePort userPersistencePort;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor;
    private final SlackNotificationAdapter slackAdapter;

    public SlackInteractionHandler(
            SlackSignatureVerifier signatureVerifier,
            DecideApprovalUseCase decideApprovalUseCase,
            CoordinationProtocolPort coordinationProtocol,
            UserPersistencePort userPersistencePort,
            ObjectMapper objectMapper,
            @Qualifier("taskExecutor") Executor taskExecutor,
            SlackNotificationAdapter slackAdapter) {
        this.signatureVerifier = signatureVerifier;
        this.decideApprovalUseCase = decideApprovalUseCase;
        this.coordinationProtocol = coordinationProtocol;
        this.userPersistencePort = userPersistencePort;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.slackAdapter = slackAdapter;
    }

    // ── Endpoint ────────────────────────────────────────────────

    @PostMapping(value = "/interactions", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> handleInteraction(
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature,
            HttpServletRequest request) throws IOException {

        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!signatureVerifier.verify(timestamp, rawBody, signature)) {
            log.warn("[InteractionHandler] Signature verification failed");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            String payloadJson = rawBody;
            if (rawBody.startsWith("payload=")) {
                payloadJson = java.net.URLDecoder.decode(
                        rawBody.substring("payload=".length()), "UTF-8");
            }

            JsonNode payload = objectMapper.readTree(payloadJson);
            if (!"block_actions".equals(payload.path("type").asText())) {
                return ResponseEntity.ok("");
            }

            JsonNode actions = payload.path("actions");
            if (actions.isEmpty()) {
                return ResponseEntity.ok("");
            }

            JsonNode action = actions.get(0);
            String actionId = action.path("action_id").asText();
            String actionValue = action.path("value").asText();

            // Idempotency guard
            String channel = payload.path("channel").path("id").asText();
            String messageTs = payload.path("message").path("ts").asText();
            String idempotencyKey = channel + ":" + messageTs + ":" + actionId;

            if (processedInteractions.putIfAbsent(idempotencyKey, Boolean.TRUE) != null) {
                log.info("[InteractionHandler] Duplicate interaction ignored: {}", idempotencyKey);
                return ResponseEntity.ok("");
            }

            if (processedInteractions.size() > MAX_PROCESSED_CACHE) {
                processedInteractions.clear();
            }

            String slackUserId = payload.path("user").path("id").asText();
            String teamId = payload.path("team").path("id").asText();

            log.info("[InteractionHandler] action_id={} value={} user={}", actionId, actionValue, slackUserId);

            // Route action — all processing on background thread
            if (actionId.startsWith("slot_select_")) {
                taskExecutor.execute(() -> handleSlotSelection(
                        actionId, actionValue, slackUserId, teamId, channel, messageTs));

            } else if (actionId.startsWith("reject_coords_")) {
                taskExecutor.execute(() -> handleEarlyRejection(
                        actionId, slackUserId, teamId, channel, messageTs));

            } else if ("approve_action".equals(actionId)) {
                taskExecutor.execute(() -> handleApproval(
                        slackUserId, teamId, actionValue, true, channel, messageTs, payload));

            } else if ("reject_action".equals(actionId)) {
                taskExecutor.execute(() -> handleApproval(
                        slackUserId, teamId, actionValue, false, channel, messageTs, payload));

            } else {
                log.warn("[InteractionHandler] Unknown action_id={}", actionId);
            }

            return ResponseEntity.ok("");

        } catch (Exception e) {
            log.warn("[InteractionHandler] Error: {}", e.getMessage());
            return ResponseEntity.ok("");
        }
    }

    // ── Background: Slot Selection ──────────────────────────────

    private void handleSlotSelection(String actionId, String value, String slackUserId,
            String teamId, String channel, String messageTs) {
        try {
            // 1. Parse coordination ID from action_id: slot_select_{coordId}_{index}
            String withoutPrefix = actionId.substring("slot_select_".length());
            int lastUnderscore = withoutPrefix.lastIndexOf('_');
            String coordIdStr = withoutPrefix.substring(0, lastUnderscore);

            // 2. Parse slot from value: startMs_endMs
            String[] parts = value.split("_");
            Instant start = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            Instant end = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            TimeSlot selectedSlot = new TimeSlot(start, end);
            CoordinationId coordId = new CoordinationId(UUID.fromString(coordIdStr));

            // 3. Format slot details
            ZonedDateTime startZdt = start.atZone(IST);
            ZonedDateTime endZdt = end.atZone(IST);
            ZonedDateTime now = ZonedDateTime.now(IST);

            // ── R1: Delete requester's "Sent available slots" message ──
            String requesterSlackId = coordinationProtocol.getMetadata(coordId, "requester_slack_id");
            String requesterNotifTs = coordinationProtocol.getMetadata(coordId, "requester_notification_ts");
            if (requesterSlackId != null && requesterNotifTs != null) {
                slackAdapter.deleteMessage(requesterSlackId, new WorkspaceId(teamId), requesterNotifTs);
            }

            // 4. Delete old slot selection card and repost "Selected Time Slot" with
            // waiting
            String statusText = "🕐 *Selected Time Slot*\n\n"
                    + "📅 " + startZdt.format(DATE_FMT) + "\n"
                    + "🕐 " + startZdt.format(TIME_FMT) + " – " + endZdt.format(TIME_FMT) + "\n\n"
                    + "_Selected at " + now.format(TIMESTAMP_FMT) + "_\n"
                    + "⏳ Waiting for approval...";

            String newTs = deleteAndRepost(channel, messageTs, channel, statusText, "#3AA3E3", teamId);
            if (newTs != null) {
                coordinationProtocol.updateMetadata(coordId, "selected_slot_ts", newTs);
            }
            // Store slot info for clean reconstruction during approval (I2)
            String slotInfo = parts[0] + "_" + parts[1] + "_" + now.format(TIMESTAMP_FMT);
            coordinationProtocol.updateMetadata(coordId, "selected_slot_info", slotInfo);

            // 5. Process the slot selection (domain logic — triggers approval flow)
            coordinationProtocol.selectSlot(coordId, selectedSlot);
            log.info("[InteractionHandler] Slot selection processed for coordination={}", coordId);

        } catch (Exception e) {
            log.warn("[InteractionHandler] Slot selection failed: {}", e.getMessage());
        }
    }

    // ── Background: Approval Decision ───────────────────────────

    private void handleApproval(String slackUserId, String teamId, String actionValue,
            boolean approved, String channel, String messageTs, JsonNode payload) {
        try {
            // Parse approvalId from value: {approvalId}[:{coordinationId}]
            String approvalIdStr = actionValue;
            if (actionValue.contains(":")) {
                approvalIdStr = actionValue.split(":")[0];
            }

            // 2. Streamlined Flow: Immediate deletion of interactive card, skip status
            // reposts
            slackAdapter.deleteMessage(slackUserId, new WorkspaceId(teamId), messageTs);
            log.info(
                    "[InteractionHandler] Decision processed (approved={}). Deleting card and skipping intermediate status updates.",
                    approved);

            // Skip "final_status_ts" metadata as we no longer repost mid-status cards
            // Skip "I2" repost logic as we want to go directly to "Meeting Confirmed"
            // The CoordinationCompletedListener will handle final notifications and
            // cleanup.

            // 3. Process the approval (domain logic)
            Optional<User> userOpt = userPersistencePort.findBySlackUserId(
                    new SlackUserId(slackUserId), new WorkspaceId(teamId));

            if (userOpt.isEmpty()) {
                log.warn("[InteractionHandler] No registered user for Slack interaction from user={}", slackUserId);
                return;
            }

            com.coagent4u.shared.UserId userId = userOpt.get().getUserId();
            ApprovalId approvalId = new ApprovalId(UUID.fromString(approvalIdStr));
            ApprovalStatus decision = approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;

            decideApprovalUseCase.decide(approvalId, userId, decision);
            log.info("[InteractionHandler] Approval {} {} by user={}", approvalIdStr, decision, slackUserId);

        } catch (Exception e) {
            log.warn("[InteractionHandler] Approval processing failed: {}", e.getMessage());
        }
    }

    // ── Delete and Repost ───────────────────────────────────────

    /**
     * @return the timestamp of the newly posted/updated message
     */
    private String deleteAndRepost(String channel, String messageTs,
            String repostChannel, String statusText, String color, String teamId) {
        try {
            String newPayload = buildStatusCard(repostChannel, statusText, color);
            boolean deleted = slackAdapter.deleteMessage(channel, new WorkspaceId(teamId), messageTs);

            if (deleted) {
                return slackAdapter.postToSlack(newPayload, repostChannel, new WorkspaceId(teamId));
            } else {
                log.info("[InteractionHandler] Fallback to chat.update for channel={} ts={}", channel, messageTs);
                String updatePayload = buildFallbackUpdatePayload(channel, messageTs, statusText, color);
                return slackAdapter.updateMessage(channel, new WorkspaceId(teamId), messageTs, updatePayload);
            }
        } catch (Exception e) {
            log.warn("[InteractionHandler] Delete-and-repost failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Payload Builders ────────────────────────────────────────

    private String buildStatusCard(String channel, String statusText, String color) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);

            ArrayNode attachments = objectMapper.createArrayNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("color", color);

            ArrayNode blocks = objectMapper.createArrayNode();

            ObjectNode section = objectMapper.createObjectNode();
            section.put("type", "section");
            ObjectNode textObj = objectMapper.createObjectNode();
            textObj.put("type", "mrkdwn");
            textObj.put("text", statusText);
            section.set("text", textObj);
            blocks.add(section);

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("[InteractionHandler] Error building status card: {}", e.getMessage());
            return "{\"channel\":\"" + channel + "\", \"text\":\"" + statusText + "\"}";
        }
    }

    private String buildFallbackUpdatePayload(String channel, String ts,
            String statusText, String color) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);
            root.put("ts", ts);

            ArrayNode attachments = objectMapper.createArrayNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("color", color);

            ArrayNode blocks = objectMapper.createArrayNode();

            ObjectNode section = objectMapper.createObjectNode();
            section.put("type", "section");
            ObjectNode textObj = objectMapper.createObjectNode();
            textObj.put("type", "mrkdwn");
            textObj.put("text", statusText);
            section.set("text", textObj);
            blocks.add(section);

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"channel\":\"" + channel + "\", \"ts\":\"" + ts + "\", \"text\":\"" + statusText + "\"}";
        }
    }

    /**
     * Extracts meeting details (date, time, participant) from the interaction
     * payload's original message blocks.
     */
    private void handleEarlyRejection(String actionId, String slackUserId, String teamId, String channel,
            String messageTs) {
        try {
            // Action ID = reject_coords_{UUID}
            String uuidStr = actionId.substring("reject_coords_".length());
            CoordinationId coordId = new CoordinationId(UUID.fromString(uuidStr));

            // Resolve Invitee Agent from Metadata
            String inviteeAgentIdStr = coordinationProtocol.getMetadata(coordId, "invitee_agent_id");
            if (inviteeAgentIdStr == null) {
                log.warn("[InteractionHandler] No invitee_agent_id in metadata for {}", coordId);
                return;
            }
            AgentId inviteeAgentId = new AgentId(UUID.fromString(inviteeAgentIdStr));

            log.info("[InteractionHandler] Processing early rejection for coordination={} by invitee={}", coordId,
                    inviteeAgentId);

            // Trigger rejection in domain
            coordinationProtocol.handleApproval(coordId, inviteeAgentId, false);

        } catch (Exception e) {
            log.warn("[InteractionHandler] Early rejection failed: {}", e.getMessage());
        }
    }

    private String extractMeetingDetails(JsonNode payload) {
        try {
            JsonNode attachments = payload.path("message").path("attachments");
            if (!attachments.isArray() || attachments.isEmpty()) {
                return "";
            }

            JsonNode blocks = attachments.get(0).path("blocks");
            if (!blocks.isArray()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                JsonNode block = blocks.get(i);
                if ("section".equals(block.path("type").asText())) {
                    String cleanText = block.path("text").path("text").asText("");
                    // Strip "Waiting for approval" line
                    cleanText = cleanText.replaceAll("(?i)⏳\\s*Waiting for approval\\s*\\.*", "");
                    // Strip "Approve or reject this meeting time." line (R2)
                    cleanText = cleanText.replaceAll("(?i)Approve or reject this meeting time\\.?", "");
                    cleanText = cleanText.trim();
                    if (!cleanText.isEmpty()) {
                        sb.append(cleanText).append("\n\n");
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.debug("[InteractionHandler] Could not extract meeting details: {}", e.getMessage());
        }
        return "";
    }

    private CoordinationId resolveCoordinationId(JsonNode payload) {
        try {
            JsonNode actions = payload.path("actions");
            if (actions.isArray() && !actions.isEmpty()) {
                String actionId = actions.get(0).path("action_id").asText("");
                if (actionId.startsWith("slot_select_")) {
                    String withoutPrefix = actionId.substring("slot_select_".length());
                    int lastUnderscore = withoutPrefix.lastIndexOf('_');
                    return new CoordinationId(UUID.fromString(withoutPrefix.substring(0, lastUnderscore)));
                }
            }

            // Try parsing from button value if action_id didn't help
            if (actions.isArray() && !actions.isEmpty()) {
                String value = actions.get(0).path("value").asText("");
                if (value.contains(":")) {
                    String coordIdPart = value.substring(value.indexOf(":") + 1);
                    return new CoordinationId(UUID.fromString(coordIdPart));
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
