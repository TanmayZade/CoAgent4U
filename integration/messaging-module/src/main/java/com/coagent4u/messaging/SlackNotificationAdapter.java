package com.coagent4u.messaging;

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
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;
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

    private final WebClient webClient;
    private final CoagentProperties properties;
    private final ObjectMapper objectMapper;

    public SlackNotificationAdapter(
            WebClient.Builder webClientBuilder,
            CoagentProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl("https://slack.com/api")
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendMessage(SlackUserId slackUserId, WorkspaceId workspaceId, String message) {
        String payload = buildBlockKitPayload(slackUserId.value(), message);
        postToSlack(payload, slackUserId.value());
    }

    @Override
    public void sendApprovalRequest(SlackUserId slackUserId, WorkspaceId workspaceId,
            String proposalText, String approvalId) {
        String payload = buildApprovalPayload(slackUserId.value(), proposalText, approvalId);
        postToSlack(payload, slackUserId.value());
    }

    /**
     * Posts a JSON payload to Slack's chat.postMessage API.
     */
    private void postToSlack(String payload, String userId) {
        try {
            String response = webClient.post()
                    .uri("/chat.postMessage")
                    .header("Authorization", "Bearer " + properties.getSlack().getBotToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            if (!responseJson.path("ok").asBoolean(false)) {
                String error = responseJson.path("error").asText("unknown_error");
                log.error("Slack API error: {} for user={}", error, userId);

                if ("not_authed".equals(error) || "invalid_auth".equals(error)
                        || "account_inactive".equals(error)) {
                    throw new NotificationFailureException("Slack auth failure: " + error);
                }
                if ("ratelimited".equals(error)) {
                    throw new ExternalServiceUnavailableException("Slack", "Rate limited");
                }
                throw new NotificationFailureException("Slack API error: " + error);
            }

            log.info("Slack message sent to user={}", userId);

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

    /**
     * Builds a plain text Block Kit message.
     */
    private String buildBlockKitPayload(String channel, String text) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);
            root.put("text", text);

            ArrayNode blocks = objectMapper.createArrayNode();
            ObjectNode section = objectMapper.createObjectNode();
            section.put("type", "section");
            ObjectNode textObj = objectMapper.createObjectNode();
            textObj.put("type", "mrkdwn");
            textObj.put("text", text);
            section.set("text", textObj);
            blocks.add(section);

            root.set("blocks", blocks);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build Block Kit payload", e);
        }
    }

    /**
     * Builds an interactive Block Kit payload with [Approve] and [Reject] buttons.
     */
    private String buildApprovalPayload(String channel, String proposalText, String approvalId) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);
            root.put("text", "📋 Approval Request: " + proposalText);

            ArrayNode blocks = objectMapper.createArrayNode();

            // Header section
            ObjectNode header = objectMapper.createObjectNode();
            header.put("type", "header");
            ObjectNode headerText = objectMapper.createObjectNode();
            headerText.put("type", "plain_text");
            headerText.put("text", "📋 Approval Request");
            header.set("text", headerText);
            blocks.add(header);

            // Proposal details section
            ObjectNode section = objectMapper.createObjectNode();
            section.put("type", "section");
            ObjectNode sectionText = objectMapper.createObjectNode();
            sectionText.put("type", "mrkdwn");
            sectionText.put("text", proposalText);
            section.set("text", sectionText);
            blocks.add(section);

            // Divider
            ObjectNode divider = objectMapper.createObjectNode();
            divider.put("type", "divider");
            blocks.add(divider);

            // Actions block with Approve and Reject buttons
            ObjectNode actions = objectMapper.createObjectNode();
            actions.put("type", "actions");
            actions.put("block_id", "approval_" + approvalId);

            ArrayNode elements = objectMapper.createArrayNode();

            // Approve button
            ObjectNode approveBtn = objectMapper.createObjectNode();
            approveBtn.put("type", "button");
            ObjectNode approveText = objectMapper.createObjectNode();
            approveText.put("type", "plain_text");
            approveText.put("text", "✅ Approve");
            approveBtn.set("text", approveText);
            approveBtn.put("style", "primary");
            approveBtn.put("action_id", "approve_action");
            approveBtn.put("value", approvalId);
            elements.add(approveBtn);

            // Reject button
            ObjectNode rejectBtn = objectMapper.createObjectNode();
            rejectBtn.put("type", "button");
            ObjectNode rejectText = objectMapper.createObjectNode();
            rejectText.put("type", "plain_text");
            rejectText.put("text", "❌ Reject");
            rejectBtn.set("text", rejectText);
            rejectBtn.put("style", "danger");
            rejectBtn.put("action_id", "reject_action");
            rejectBtn.put("value", approvalId);
            elements.add(rejectBtn);

            actions.set("elements", elements);
            blocks.add(actions);

            root.set("blocks", blocks);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build approval payload", e);
        }
    }

    // ── Slot Selection Card ──

    @Override
    public void sendSlotSelection(SlackUserId slackUserId, WorkspaceId workspaceId,
            String coordinationId, java.util.List<com.coagent4u.shared.TimeSlot> slots,
            String requesterMention) {
        log.info("[NotificationService] Sending slot selection card to user={} for coordination={}",
                slackUserId.value(), coordinationId);
        String payload = buildSlotSelectionPayload(slackUserId.value(), coordinationId, slots, requesterMention);
        postToSlack(payload, slackUserId.value());
    }

    /**
     * Builds a Slack Block Kit payload with buttons for each available time slot.
     */
    private String buildSlotSelectionPayload(String channel, String coordinationId,
            java.util.List<com.coagent4u.shared.TimeSlot> slots, String requesterMention) {
        try {
            java.time.ZoneId ist = java.time.ZoneId.of("Asia/Kolkata");
            java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy");
            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);
            root.put("text", requesterMention + " wants to schedule a meeting with you");

            ArrayNode blocks = objectMapper.createArrayNode();

            // Header section with requester name
            ObjectNode headerSection = objectMapper.createObjectNode();
            headerSection.put("type", "section");
            ObjectNode headerText = objectMapper.createObjectNode();
            headerText.put("type", "mrkdwn");
            headerText.put("text",
                    "*" + requesterMention
                            + " wants to schedule a meeting with you.*\n\nPlease select one of the available slots below.");
            headerSection.set("text", headerText);
            blocks.add(headerSection);

            // Divider
            ObjectNode divider = objectMapper.createObjectNode();
            divider.put("type", "divider");
            blocks.add(divider);

            // Slot buttons (in groups of 5 — Slack limit per actions block)
            int slotIndex = 0;
            ArrayNode currentActions = null;
            ObjectNode currentActionsBlock = null;

            for (com.coagent4u.shared.TimeSlot slot : slots) {
                if (slotIndex % 5 == 0) {
                    currentActionsBlock = objectMapper.createObjectNode();
                    currentActionsBlock.put("type", "actions");
                    currentActions = objectMapper.createArrayNode();
                    currentActionsBlock.set("elements", currentActions);
                    blocks.add(currentActionsBlock);
                }

                java.time.ZonedDateTime startZdt = slot.start().atZone(ist);
                java.time.ZonedDateTime endZdt = slot.end().atZone(ist);

                String label = startZdt.format(timeFmt) + " – " + endZdt.format(timeFmt);
                if (slotIndex == 0) {
                    // Show date on first slot
                    label = startZdt.format(dateFmt) + "\n" + label;
                }

                ObjectNode button = objectMapper.createObjectNode();
                button.put("type", "button");
                ObjectNode btnText = objectMapper.createObjectNode();
                btnText.put("type", "plain_text");
                btnText.put("text", "🕐 " + startZdt.format(timeFmt) + "–" + endZdt.format(timeFmt));
                button.set("text", btnText);
                // action_id encodes: slot_select_{coordinationId}_{slotIndex}
                button.put("action_id", "slot_select_" + coordinationId + "_" + slotIndex);
                // value encodes: start_end as epoch millis
                button.put("value", slot.start().toEpochMilli() + "_" + slot.end().toEpochMilli());
                currentActions.add(button);

                slotIndex++;
            }

            root.set("blocks", blocks);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new NotificationFailureException("Failed to build slot selection payload", e);
        }
    }
}
