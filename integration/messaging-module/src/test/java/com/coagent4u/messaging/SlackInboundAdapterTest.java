package com.coagent4u.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.in.HandleMessageUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.security.SlackSignatureVerifier;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.UserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlackInboundAdapter Tests")
class SlackInboundAdapterTest {

    @Mock
    private SlackSignatureVerifier signatureVerifier;
    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private AgentPersistencePort agentPersistencePort;
    @Mock
    private HandleMessageUseCase handleMessageUseCase;

    private SlackInboundAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Inline executor — runs tasks on the calling thread for deterministic testing.
     */
    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        adapter = new SlackInboundAdapter(
                signatureVerifier, userPersistencePort, agentPersistencePort,
                handleMessageUseCase, objectMapper, directExecutor);
    }

    @Test
    @DisplayName("rejects request with invalid signature")
    void rejectsInvalidSignature() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(false);

        ResponseEntity<String> response = adapter.handleEvent("123456", "v0=bad", "{}");

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @DisplayName("handles URL verification challenge")
    void handlesUrlVerification() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);

        String body = """
                {"type":"url_verification","challenge":"test_challenge_token"}
                """;

        ResponseEntity<String> response = adapter.handleEvent("123456", "v0=valid", body);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("test_challenge_token", response.getBody());
    }

    @Test
    @DisplayName("acknowledges event callback with 200 immediately")
    void acknowledgesEventCallback() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);

        String body = """
                {
                  "type":"event_callback",
                  "event_id":"Ev123",
                  "team_id":"T123",
                  "event":{"type":"message","user":"U123","text":"schedule meeting"}
                }
                """;

        ResponseEntity<String> response = adapter.handleEvent("123456", "v0=valid", body);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("deduplicates events by event_id")
    void deduplicatesEvents() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);

        String body = """
                {
                  "type":"event_callback",
                  "event_id":"Ev_dedup_test",
                  "team_id":"T123",
                  "event":{"type":"message","user":"U123","text":"hello"}
                }
                """;

        // First call — processed
        adapter.handleEvent("123456", "v0=valid", body);
        // Second call — deduplicated
        ResponseEntity<String> response = adapter.handleEvent("123456", "v0=valid", body);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("processes message event and delegates to HandleMessageUseCase")
    void processesMessageAndDelegates() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);

        UserId userId = new UserId(UUID.randomUUID());
        AgentId agentId = new AgentId(UUID.randomUUID());

        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn(userId);

        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn(agentId);

        when(userPersistencePort.findBySlackUserId(any(SlackUserId.class), any(WorkspaceId.class)))
                .thenReturn(Optional.of(mockUser));
        when(agentPersistencePort.findByUserId(userId)).thenReturn(Optional.of(mockAgent));

        String body = """
                {
                  "type":"event_callback",
                  "event_id":"Ev_delegate_test",
                  "team_id":"T123",
                  "event":{"type":"message","user":"U123","text":"add meeting tomorrow"}
                }
                """;

        adapter.handleEvent("123456", "v0=valid", body);

        // directExecutor runs inline, so handleMessage should have been called
        verify(handleMessageUseCase).handleMessage(agentId, "add meeting tomorrow");
    }

    @Test
    @DisplayName("handles unknown user gracefully")
    void handlesUnknownUserGracefully() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);
        when(userPersistencePort.findBySlackUserId(any(SlackUserId.class), any(WorkspaceId.class)))
                .thenReturn(Optional.empty());

        String body = """
                {
                  "type":"event_callback",
                  "event_id":"Ev_unknown_user",
                  "team_id":"T123",
                  "event":{"type":"message","user":"U_unknown","text":"hello"}
                }
                """;

        // Should not throw
        assertDoesNotThrow(() -> adapter.handleEvent("123456", "v0=valid", body));

        verify(handleMessageUseCase, never()).handleMessage(any(), any());
    }
}
