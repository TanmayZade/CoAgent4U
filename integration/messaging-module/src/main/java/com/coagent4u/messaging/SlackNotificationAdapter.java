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
 *
 * <p>
 * Error mapping:
 * </p>
 * <ul>
 * <li>Slack 403 → {@link NotificationFailureException}</li>
 * <li>Slack 429 / 5xx → {@link ExternalServiceUnavailableException}</li>
 * </ul>
 */
@Component
public class SlackNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationAdapter.class);
    private static final String SLACK_API_URL = "https://slack.com/api/chat.postMessage";

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
        try {
            String payload = buildBlockKitPayload(slackUserId.value(), message);

            String response = webClient.post()
                    .uri("/chat.postMessage")
                    .header("Authorization", "Bearer " + properties.getSlack().getBotToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Slack returns 200 even on errors — check the "ok" field
            JsonNode responseJson = objectMapper.readTree(response);
            if (!responseJson.path("ok").asBoolean(false)) {
                String error = responseJson.path("error").asText("unknown_error");
                log.error("Slack API error: {} for user={}", error, slackUserId.value());

                if ("not_authed".equals(error) || "invalid_auth".equals(error)
                        || "account_inactive".equals(error)) {
                    throw new NotificationFailureException(
                            "Slack auth failure: " + error);
                }
                if ("ratelimited".equals(error)) {
                    throw new ExternalServiceUnavailableException("Slack", "Rate limited");
                }
                throw new NotificationFailureException("Slack API error: " + error);
            }

            log.info("Slack message sent to user={}", slackUserId.value());

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
            throw e; // re-throw domain exceptions
        } catch (Exception e) {
            throw new NotificationFailureException(
                    "Failed to send Slack message: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a Slack Block Kit message payload.
     */
    private String buildBlockKitPayload(String channel, String text) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("channel", channel);
            root.put("text", text); // fallback for notifications

            // Block Kit section
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
}
