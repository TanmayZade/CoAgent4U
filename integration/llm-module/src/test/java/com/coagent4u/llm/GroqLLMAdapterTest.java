package com.coagent4u.llm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.AgentId;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroqLLMAdapter Tests")
class GroqLLMAdapterTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GroqLLMAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentId agentId = new AgentId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        CoagentProperties props = new CoagentProperties();
        props.getLlm().setEnabled(true);
        props.getLlm().setGroqApiKey("gsk_test_key");
        props.getLlm().setModel("llama3-8b-8192");

        adapter = new GroqLLMAdapter(webClientBuilder, props, objectMapper);
    }

    @Test
    @DisplayName("classifies ADD_EVENT intent")
    void classifiesAddEvent() {
        setupWebClientMocks(groqResponse("ADD_EVENT"));

        Optional<String> intent = adapter.classifyIntent(agentId, "Schedule a dentist appointment tomorrow at 2pm");

        assertTrue(intent.isPresent());
        assertEquals("ADD_EVENT", intent.get());
    }

    @Test
    @DisplayName("classifies VIEW_SCHEDULE intent")
    void classifiesViewSchedule() {
        setupWebClientMocks(groqResponse("VIEW_SCHEDULE"));

        Optional<String> intent = adapter.classifyIntent(agentId, "What's on my calendar today?");

        assertTrue(intent.isPresent());
        assertEquals("VIEW_SCHEDULE", intent.get());
    }

    @Test
    @DisplayName("classifies SCHEDULE_MEETING intent")
    void classifiesScheduleMeeting() {
        setupWebClientMocks(groqResponse("SCHEDULE_MEETING"));

        Optional<String> intent = adapter.classifyIntent(agentId, "Set up a meeting with Alice");

        assertTrue(intent.isPresent());
        assertEquals("SCHEDULE_MEETING", intent.get());
    }

    @Test
    @DisplayName("returns UNKNOWN for unrecognized LLM response")
    void returnsUnknownForBadResponse() {
        setupWebClientMocks(groqResponse("SOME_RANDOM_RESPONSE"));

        Optional<String> intent = adapter.classifyIntent(agentId, "What is the meaning of life?");

        assertTrue(intent.isPresent());
        assertEquals("UNKNOWN", intent.get());
    }

    @Test
    @DisplayName("returns empty on API error — fallback to Tier 1")
    void returnsEmptyOnApiError() {
        WebClientResponseException ex = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));

        Optional<String> intent = adapter.classifyIntent(agentId, "hello");

        assertTrue(intent.isEmpty(), "Should return empty on API failure for Tier 1 fallback");
    }

    @Test
    @DisplayName("returns empty when LLM is disabled")
    void returnsEmptyWhenDisabled() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        CoagentProperties props = new CoagentProperties();
        props.getLlm().setEnabled(false);

        GroqLLMAdapter disabledAdapter = new GroqLLMAdapter(webClientBuilder, props, objectMapper);

        Optional<String> intent = disabledAdapter.classifyIntent(agentId, "hello");

        assertTrue(intent.isEmpty());
    }

    @Test
    @DisplayName("summarizeSchedule returns summary text")
    void summarizesSchedule() {
        setupWebClientMocks(groqResponse("Here's your schedule for today:\n• 9 AM - Standup\n• 2 PM - Design review"));

        String summary = adapter.summarizeSchedule(agentId, "[{\"title\":\"Standup\",\"start\":\"09:00\"}]");

        assertNotNull(summary);
        assertFalse(summary.isBlank());
    }

    @Test
    @DisplayName("summarizeSchedule returns fallback on error")
    void summarizeScheduleFallbackOnError() {
        WebClientResponseException ex = WebClientResponseException.create(
                503, "Service Unavailable", null, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));

        String summary = adapter.summarizeSchedule(agentId, "[]");

        assertEquals("Unable to summarize schedule at this time.", summary);
    }

    // ── Helpers ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void setupWebClientMocks(String responseBody) {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
    }

    private String groqResponse(String content) {
        return """
                {
                  "id": "chatcmpl-test",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      }
                    }
                  ]
                }
                """.formatted(content);
    }
}
