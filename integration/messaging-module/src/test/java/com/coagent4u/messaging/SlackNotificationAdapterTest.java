package com.coagent4u.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.coagent4u.common.exception.NotificationFailureException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.WorkspaceId;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlackNotificationAdapter Tests")
class SlackNotificationAdapterTest {

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

    private SlackNotificationAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        CoagentProperties props = new CoagentProperties();
        props.getSlack().setBotToken("xoxb-test-token");

        adapter = new SlackNotificationAdapter(webClientBuilder, props, objectMapper);
    }

    @Test
    @DisplayName("sends message and parses success response")
    void sendsMessageSuccessfully() {
        setupWebClientMocks("{\"ok\":true,\"channel\":\"D123\",\"ts\":\"123.456\"}");

        assertDoesNotThrow(() -> adapter.sendMessage(new SlackUserId("U123"), new WorkspaceId("T123"), "Hello!"));
    }

    @Test
    @DisplayName("throws NotificationFailureException on Slack API error")
    void throwsOnSlackApiError() {
        setupWebClientMocks("{\"ok\":false,\"error\":\"channel_not_found\"}");

        assertThrows(NotificationFailureException.class,
                () -> adapter.sendMessage(new SlackUserId("U123"), new WorkspaceId("T123"), "Hello!"));
    }

    @Test
    @DisplayName("throws NotificationFailureException on auth error")
    void throwsOnAuthError() {
        setupWebClientMocks("{\"ok\":false,\"error\":\"not_authed\"}");

        NotificationFailureException ex = assertThrows(NotificationFailureException.class,
                () -> adapter.sendMessage(new SlackUserId("U123"), new WorkspaceId("T123"), "Hello!"));

        assertTrue(ex.getMessage().contains("auth"));
    }

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
}
