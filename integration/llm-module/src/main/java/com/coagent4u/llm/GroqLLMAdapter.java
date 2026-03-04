package com.coagent4u.llm;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.common.exception.LLMUnavailableException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.AgentId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Groq LLM adapter — implements {@link LLMPort} for Tier 2 intent
 * classification.
 *
 * <p>
 * On failure, returns {@link Optional#empty()} so the caller falls back
 * to Tier 1 rule-based parsing. Never throws — all errors are caught and
 * logged.
 * </p>
 */
@Component
public class GroqLLMAdapter implements LLMPort {

    private static final Logger log = LoggerFactory.getLogger(GroqLLMAdapter.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String INTENT_SYSTEM_PROMPT = """
            You are a calendar assistant intent classifier.
            Classify the user's message into exactly ONE of these intents:
            - ADD_EVENT: User wants to add/create/schedule a personal event
            - VIEW_SCHEDULE: User wants to see/view/check their schedule or calendar
            - SCHEDULE_MEETING: User wants to schedule a meeting with another person
            - CANCEL_EVENT: User wants to cancel/delete/remove an event
            - UNKNOWN: The message doesn't match any of the above

            Respond with ONLY the intent name, nothing else.
            """;

    private final WebClient webClient;
    private final CoagentProperties properties;
    private final ObjectMapper objectMapper;

    public GroqLLMAdapter(
            WebClient.Builder webClientBuilder,
            CoagentProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(GROQ_API_URL).build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> classifyIntent(AgentId agentId, String rawText) {
        if (!properties.getLlm().isEnabled()) {
            log.debug("LLM disabled, skipping intent classification for agent={}", agentId);
            return Optional.empty();
        }

        try {
            String payload = buildChatPayload(INTENT_SYSTEM_PROMPT, rawText);

            String response = webClient.post()
                    .uri("")
                    .header("Authorization", "Bearer " + properties.getLlm().getGroqApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String intent = extractContent(response);
            if (intent == null || intent.isBlank()) {
                log.warn("Empty LLM response for agent={}", agentId);
                return Optional.empty();
            }

            // Validate intent is a known value
            String normalized = intent.trim().toUpperCase();
            if (isValidIntent(normalized)) {
                log.info("LLM classified intent={} for agent={}", normalized, agentId);
                return Optional.of(normalized);
            }

            log.warn("LLM returned unknown intent='{}', treating as UNKNOWN", intent);
            return Optional.of("UNKNOWN");

        } catch (WebClientResponseException e) {
            log.error("Groq API error status={}: body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("LLM classification failed for agent={}", agentId, e);
            return Optional.empty();
        }
    }

    @Override
    public String summarizeSchedule(AgentId agentId, String scheduleJson) {
        if (!properties.getLlm().isEnabled()) {
            return "Schedule summary unavailable (LLM disabled).";
        }

        String systemPrompt = "You are a calendar assistant. Summarize the following schedule "
                + "in a friendly, concise, human-readable format. Use bullet points for events.";

        try {
            String payload = buildChatPayload(systemPrompt, scheduleJson);

            String response = webClient.post()
                    .uri("")
                    .header("Authorization", "Bearer " + properties.getLlm().getGroqApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String summary = extractContent(response);
            if (summary == null || summary.isBlank()) {
                return "Unable to summarize schedule at this time.";
            }
            return summary;

        } catch (Exception e) {
            log.error("Schedule summarization failed for agent={}", agentId, e);
            return "Unable to summarize schedule at this time.";
        }
    }

    // ── Internal helpers ──────────────────────────────────────

    private String buildChatPayload(String systemPrompt, String userMessage) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", properties.getLlm().getModel());
            root.put("temperature", 0.1);
            root.put("max_tokens", 100);

            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode system = objectMapper.createObjectNode();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.add(system);

            ObjectNode user = objectMapper.createObjectNode();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);

            root.set("messages", messages);
            return objectMapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new LLMUnavailableException("Failed to build chat payload", e);
        }
    }

    private String extractContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("Failed to parse Groq response", e);
            return null;
        }
    }

    private boolean isValidIntent(String intent) {
        return "ADD_EVENT".equals(intent)
                || "VIEW_SCHEDULE".equals(intent)
                || "SCHEDULE_MEETING".equals(intent)
                || "CANCEL_EVENT".equals(intent)
                || "UNKNOWN".equals(intent);
    }
}
