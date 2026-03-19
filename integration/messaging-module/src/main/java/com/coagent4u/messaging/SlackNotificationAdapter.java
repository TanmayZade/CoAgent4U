package com.coagent4u.messaging;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.common.exception.ExternalServiceUnavailableException;
import com.coagent4u.common.exception.NotificationFailureException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.domain.WorkspaceInstallation;
import com.coagent4u.user.port.out.WorkspaceInstallationPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Sends Slack messages via the Slack Web API (chat.postMessage).
 * Implements {@link NotificationPort} from the user domain.
 */
@Component
public class SlackNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationAdapter.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final WebClient webClient;
    private final CoagentProperties properties;
    private final WorkspaceInstallationPersistencePort workspaceInstallationPersistencePort;
    private final ObjectMapper objectMapper;

    public SlackNotificationAdapter(
            WebClient.Builder webClientBuilder,
            CoagentProperties properties,
            WorkspaceInstallationPersistencePort workspaceInstallationPersistencePort,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://slack.com/api")
                .build();
        this.properties = properties;
        this.workspaceInstallationPersistencePort = workspaceInstallationPersistencePort;
        this.objectMapper = objectMapper;
    }

    // ── Public API ──────────────────────────────────────────────

    @Override
    public String sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String message) {
        String payload = buildBlockKitPayload(slackUserId.value(), message);
        return postToSlack(payload, slackUserId.value(), workspaceId);
    }

    @Override
    public String sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
            String proposalText, String approvalId, String coordinationId) {
        String payload = buildApprovalPayload(slackUserId.value(), proposalText, approvalId, coordinationId);
        return postToSlack(payload, slackUserId.value(), workspaceId);
    }

    @Override
    public String sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
            String coordinationId, List<TimeSlot> slots, String requesterMention) {
        log.info("[SlackAdapter] Sending slot selection card to user={} for coordination={}",
                slackUserId.value(), coordinationId);
        String payload = buildSlotSelectionPayload(slackUserId.value(), coordinationId, slots, requesterMention);
        return postToSlack(payload, slackUserId.value(), workspaceId);
    }

    @Override
    public String sendSlotPreview(SlackUserId slackUserId, WorkspaceId workspaceId,
            List<TimeSlot> slots, String inviteeMention) {
        log.info("[SlackAdapter] Sending slot preview card to requester={}", slackUserId.value());
        String payload = buildSlotPreviewPayload(slackUserId.value(), slots, inviteeMention);
        return postToSlack(payload, slackUserId.value(), workspaceId);
    }

    @Override
    public String sendStatusCard(SlackUserId slackUserId, WorkspaceId workspaceId, String statusText, String color) {
        String payload = buildStatusCardPayload(slackUserId.value(), statusText, color);
        return postToSlack(payload, slackUserId.value(), workspaceId);
    }

    /**
     * Posts a pre-built JSON payload to Slack's chat.postMessage.
     * Made public so the interaction handler can repost status cards.
     *
     * @return the message timestamp (ts) if successful
     */
    public String postToSlack(String payload, String userId, WorkspaceId workspaceId) {
        String token = getBotToken(workspaceId);
        try {
            String response = webClient.post()
                    .uri("/chat.postMessage")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            if (!responseJson.path("ok").asBoolean(false)) {
                String error = responseJson.path("error").asText("unknown_error");
                log.error("[SlackAdapter] chat.postMessage error: {} for user={}", error, userId);

                if ("not_authed".equals(error) || "invalid_auth".equals(error)
                        || "account_inactive".equals(error)) {
                    throw new NotificationFailureException("Slack auth failure: " + error);
                }
                if ("ratelimited".equals(error)) {
                    throw new ExternalServiceUnavailableException("Slack", "Rate limited");
                }
                throw new NotificationFailureException("Slack API error: " + error);
            }

            log.info("[SlackAdapter] Message posted to user={}", userId);
            return responseJson.path("ts").asText();

        } catch (WebClientResponseException.Forbidden e) {
            throw new NotificationFailureException("Slack forbidden: " + e.getMessage(), e);
        } catch (WebClientResponseException.TooManyRequests e) {
            throw new ExternalServiceUnavailableException("Slack", "Rate limited", e);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                throw new ExternalServiceUnavailableException("Slack",
                        "Server error: " + e.getStatusCode(), e);
            }
            throw new NotificationFailureException("Slack error: " + e.getMessage(), e);
        } catch (NotificationFailureException | ExternalServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationFailureException(
                    "Failed to send Slack message: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String ts) {
        return deleteMessage(slackUserId.value(), workspaceId, ts);
    }

    /**
     * Deletes a Slack message using chat.delete.
     *
     * @param channel the channel ID
     * @param workspaceId the workspace ID
     * @param ts      the message timestamp
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteMessage(String channel, WorkspaceId workspaceId, String ts) {
        String token = getBotToken(workspaceId);
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("channel", channel);
            body.put("ts", ts);

            String response = webClient.post()
                    .uri("/chat.delete")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            if (!responseJson.path("ok").asBoolean(false)) {
                String error = responseJson.path("error").asText("unknown_error");
                log.warn("[SlackAdapter] chat.delete failed: {} for channel={} ts={}", error, channel, ts);
                return false;
            }

            log.info("[SlackAdapter] Message deleted in channel={} ts={}", channel, ts);
            return true;

        } catch (Exception e) {
            log.warn("[SlackAdapter] chat.delete exception: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing Slack message using chat.update API.
     * Used as a fallback when chat.delete fails.
     *
     * @return the message timestamp (ts) if successful
     */
    public String updateMessage(String channel, WorkspaceId workspaceId, String ts, String payload) {
        String token = getBotToken(workspaceId);
        try {
            String response = webClient.post()
                    .uri("/chat.update")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            if (!responseJson.path("ok").asBoolean(false)) {
                String error = responseJson.path("error").asText("unknown_error");
                log.error("[SlackAdapter] chat.update error: {} for channel={} ts={}", error, channel, ts);
                return ts; // Return original if update failed
            } else {
                log.info("[SlackAdapter] Message updated in channel={} ts={}", channel, ts);
                return responseJson.path("ts").asText();
            }
        } catch (Exception e) {
            log.warn("[SlackAdapter] chat.update exception: {}", e.getMessage());
            return ts;
        }
    }

    private String getBotToken(WorkspaceId workspaceId) {
        if (workspaceId == null) {
            log.warn("Null workspaceId provided for bot token lookup, falling back to global token");
            return properties.getSlack().getBotToken();
        }
        return workspaceInstallationPersistencePort.findByWorkspaceId(workspaceId)
                .map(WorkspaceInstallation::botToken)
                .orElseGet(() -> {
                    log.debug("No workspace installation found for {}, falling back to global bot token", workspaceId);
                    return properties.getSlack().getBotToken();
                });
    }

    // ── Payload Builders ────────────────────────────────────────

    /**
     * Builds a plain text Block Kit message with colored sidebar.
     */
    private String buildStatusCardPayload(String channel, String text, String color) {
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
            textObj.put("text", text);
            textObj.put("type", "mrkdwn");
            section.set("text", textObj);
            blocks.add(section);

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build status card payload", e);
        }
    }

    /**
     * Builds a plain text Block Kit message with colored sidebar.
     */
    private String buildBlockKitPayload(String channel, String text) {
        return buildStatusCardPayload(channel, text, "#745EAF");
    }

    /**
     * Builds an interactive Block Kit payload with Approve / Reject buttons.
     */
    private String buildApprovalPayload(String channel, String proposalText, String approvalId, String coordinationId) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);

            ArrayNode attachments = objectMapper.createArrayNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("color", "#4A154B");

            ArrayNode blocks = objectMapper.createArrayNode();

            // Header
            String headerText = proposalText.contains("*Create Event:*")
                    ? "📋 Event Creation Request"
                    : "📋 Meeting Approval Request";
            blocks.add(headerBlock(headerText));

            // Proposal details
            blocks.add(markdownSection(proposalText));

            // Divider
            blocks.add(dividerBlock());

            // Action buttons
            ObjectNode actions = objectMapper.createObjectNode();
            actions.put("type", "actions");
            actions.put("block_id", "approval_" + approvalId);

            ArrayNode elements = objectMapper.createArrayNode();
            // Value is {approvalId}:{coordinationId}
            String btnValue = approvalId + ":" + coordinationId;
            elements.add(buttonElement("✅ Approve", "approve_action", btnValue, "primary"));
            elements.add(buttonElement("❌ Reject", "reject_action", btnValue, "danger"));
            actions.set("elements", elements);
            blocks.add(actions);

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build approval payload", e);
        }
    }

    /**
     * Builds a slot selection card — individual 1-hour slots grouped by date.
     * NO merging of consecutive slots (domain validates against original slots).
     * Buttons are rendered in a horizontal grid (one actions block per date).
     */
    private String buildSlotSelectionPayload(String channel, String coordinationId,
            List<TimeSlot> slots, String requesterMention) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);

            ArrayNode attachments = objectMapper.createArrayNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("color", "#00A896");

            ArrayNode blocks = objectMapper.createArrayNode();

            // Header
            blocks.add(headerBlock("📅 Available Time Slots"));

            // Description
            blocks.add(markdownSection(
                    "Please select a suitable time slot:"));

            // Divider
            blocks.add(dividerBlock());

            // Group slots by date (no merging — each slot is 1 hour)
            List<TimeSlot> sorted = new ArrayList<>(slots);
            sorted.sort((a, b) -> a.start().compareTo(b.start()));

            Map<String, List<TimeSlot>> byDate = new LinkedHashMap<>();
            for (TimeSlot slot : sorted) {
                ZonedDateTime startZdt = slot.start().atZone(IST);
                String dateKey = startZdt.format(DATE_FMT);
                byDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(slot);
            }

            int slotIndex = 0;
            for (Map.Entry<String, List<TimeSlot>> entry : byDate.entrySet()) {
                String dateLabel = entry.getKey();
                List<TimeSlot> dateSlots = entry.getValue();

                // Date header
                blocks.add(markdownSection("*📅 " + dateLabel + "*"));

                // All slots for this date in ONE actions block → horizontal grid
                ObjectNode actionsBlock = objectMapper.createObjectNode();
                actionsBlock.put("type", "actions");
                ArrayNode elements = objectMapper.createArrayNode();

                for (TimeSlot slot : dateSlots) {
                    ZonedDateTime startZdt = slot.start().atZone(IST);
                    ZonedDateTime endZdt = slot.end().atZone(IST);
                    String timeLabel = startZdt.format(TIME_FMT) + " – " + endZdt.format(TIME_FMT);

                    // Value = startMs_endMs (exact 1h slot — matches domain)
                    String value = slot.start().toEpochMilli() + "_" + slot.end().toEpochMilli();
                    elements.add(buttonElement(timeLabel,
                            "slot_select_" + coordinationId + "_" + slotIndex, value, null));
                    slotIndex++;
                }

                actionsBlock.set("elements", elements);
                blocks.add(actionsBlock);
            }

            // ── Footer: Reject Option ──
            blocks.add(dividerBlock());
            ObjectNode footerActions = objectMapper.createObjectNode();
            footerActions.put("type", "actions");
            ArrayNode footerElements = objectMapper.createArrayNode();
            footerElements.add(buttonElement("Reject Meeting", "reject_coords_" + coordinationId, "reject", "danger"));
            footerActions.set("elements", footerElements);
            blocks.add(footerActions);

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build slot selection payload", e);
        }
    }

    /**
     * Builds a read-only slot preview card for the requester.
     */
    private String buildSlotPreviewPayload(String channel, List<TimeSlot> slots, String inviteeMention) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);

            ArrayNode attachments = objectMapper.createArrayNode();
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("color", "#745EAF");

            ArrayNode blocks = objectMapper.createArrayNode();

            // Header
            blocks.add(headerBlock("📤 Proposed Slots"));

            // Description
            blocks.add(markdownSection(
                    "I've proposed these available time slots to " + inviteeMention + ". Waiting for their selection..."));

            // Divider
            blocks.add(dividerBlock());

            // Group slots by date
            List<TimeSlot> sorted = new ArrayList<>(slots);
            sorted.sort((a, b) -> a.start().compareTo(b.start()));

            Map<String, List<TimeSlot>> byDate = new LinkedHashMap<>();
            for (TimeSlot slot : sorted) {
                ZonedDateTime startZdt = slot.start().atZone(IST);
                String dateKey = startZdt.format(DATE_FMT);
                byDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(slot);
            }

            for (Map.Entry<String, List<TimeSlot>> entry : byDate.entrySet()) {
                String dateLabel = entry.getKey();
                List<TimeSlot> dateSlots = entry.getValue();

                // Date header
                blocks.add(markdownSection("*📅 " + dateLabel + "*"));

                // Render slots as plain text list
                StringBuilder sb = new StringBuilder();
                for (TimeSlot slot : dateSlots) {
                    ZonedDateTime startZdt = slot.start().atZone(IST);
                    ZonedDateTime endZdt = slot.end().atZone(IST);
                    String timeLabel = startZdt.format(TIME_FMT) + " – " + endZdt.format(TIME_FMT);
                    sb.append("• ").append(timeLabel).append("\n");
                }
                blocks.add(markdownSection(sb.toString().trim()));
            }

            attachment.set("blocks", blocks);
            attachments.add(attachment);
            root.set("attachments", attachments);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build slot preview payload", e);
        }
    }

    // ── Block Kit Helpers ───────────────────────────────────────

    private ObjectNode headerBlock(String text) {
        ObjectNode header = objectMapper.createObjectNode();
        header.put("type", "header");
        ObjectNode headerText = objectMapper.createObjectNode();
        headerText.put("type", "plain_text");
        headerText.put("text", text);
        header.set("text", headerText);
        return header;
    }

    private ObjectNode markdownSection(String text) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("type", "section");
        ObjectNode textObj = objectMapper.createObjectNode();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);
        section.set("text", textObj);
        return section;
    }

    private ObjectNode dividerBlock() {
        ObjectNode divider = objectMapper.createObjectNode();
        divider.put("type", "divider");
        return divider;
    }

    private ObjectNode buttonElement(String label, String actionId, String value, String style) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "button");
        ObjectNode btnText = objectMapper.createObjectNode();
        btnText.put("type", "plain_text");
        btnText.put("text", label);
        button.set("text", btnText);
        button.put("action_id", actionId);
        button.put("value", value);
        if (style != null) {
            button.put("style", style);
        }
        return button;
    }
}
