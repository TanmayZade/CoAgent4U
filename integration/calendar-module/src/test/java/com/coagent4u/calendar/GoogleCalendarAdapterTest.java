package com.coagent4u.calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.common.exception.ExternalServiceUnavailableException;
import com.coagent4u.common.exception.TokenExpiredException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.security.AesTokenEncryption;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.ServiceConnection;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleCalendarAdapter Tests")
class GoogleCalendarAdapterTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private AesTokenEncryption encryptionService;
    @Mock
    private AgentPersistencePort agentPersistencePort;
    @Mock
    private UserPersistencePort userPersistencePort;

    private GoogleCalendarAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentId agentId = new AgentId(UUID.randomUUID());
    private final UserId userId = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        CoagentProperties props = new CoagentProperties();
        adapter = new GoogleCalendarAdapter(
                webClientBuilder, props, encryptionService,
                agentPersistencePort, userPersistencePort, objectMapper);
    }

    @Test
    @DisplayName("getFreeBusy returns busy slots from Google response")
    @SuppressWarnings("unchecked")
    void getFreeBusyReturnsBusySlots() {
        setupAgentAndUser();

        String googleResponse = """
                {
                  "calendars": {
                    "primary": {
                      "busy": [
                        {"start":"2024-01-15T10:00:00Z","end":"2024-01-15T11:00:00Z"},
                        {"start":"2024-01-15T14:00:00Z","end":"2024-01-15T15:30:00Z"}
                      ]
                    }
                  }
                }
                """;

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(googleResponse));

        TimeRange range = new TimeRange(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15));
        List<TimeSlot> busy = adapter.getFreeBusy(agentId, range);

        assertEquals(2, busy.size());
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), busy.get(0).start());
        assertEquals(Instant.parse("2024-01-15T11:00:00Z"), busy.get(0).end());
    }

    @Test
    @DisplayName("createEvent returns EventId from Google response")
    @SuppressWarnings("unchecked")
    void createEventReturnsId() {
        setupAgentAndUser();

        String googleResponse = """
                {"id":"google_event_123","status":"confirmed"}
                """;

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(googleResponse));

        TimeSlot slot = new TimeSlot(
                Instant.parse("2024-01-15T10:00:00Z"),
                Instant.parse("2024-01-15T11:00:00Z"));

        EventId eventId = adapter.createEvent(agentId, slot, "Team standup");

        assertEquals("google_event_123", eventId.value());
    }

    @Test
    @DisplayName("maps Google 401 to TokenExpiredException")
    void mapsUnauthorizedToTokenExpired() {
        setupAgentAndUser();

        WebClientResponseException ex = WebClientResponseException.create(
                401, "Unauthorized", null, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));

        TimeSlot slot = new TimeSlot(
                Instant.parse("2024-01-15T10:00:00Z"),
                Instant.parse("2024-01-15T11:00:00Z"));

        assertThrows(TokenExpiredException.class, () -> adapter.createEvent(agentId, slot, "Test"));
    }

    @Test
    @DisplayName("maps Google 429 to ExternalServiceUnavailableException")
    void mapsRateLimitToUnavailable() {
        setupAgentAndUser();

        WebClientResponseException ex = WebClientResponseException.create(
                429, "Too Many Requests", null, null, null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(ex));

        TimeSlot slot = new TimeSlot(
                Instant.parse("2024-01-15T10:00:00Z"),
                Instant.parse("2024-01-15T11:00:00Z"));

        assertThrows(ExternalServiceUnavailableException.class, () -> adapter.createEvent(agentId, slot, "Test"));
    }

    @Test
    @DisplayName("throws TokenExpiredException when no active connection")
    void throwsWhenNoConnection() {
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getUserId()).thenReturn(userId);
        when(agentPersistencePort.findById(agentId)).thenReturn(Optional.of(mockAgent));

        User mockUser = mock(User.class);
        when(mockUser.activeConnectionFor("google_calendar")).thenReturn(Optional.empty());
        when(userPersistencePort.findById(userId)).thenReturn(Optional.of(mockUser));

        TimeRange range = new TimeRange(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15));

        assertThrows(TokenExpiredException.class, () -> adapter.getFreeBusy(agentId, range));
    }

    private void setupAgentAndUser() {
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getUserId()).thenReturn(userId);
        when(agentPersistencePort.findById(agentId)).thenReturn(Optional.of(mockAgent));

        ServiceConnection mockConnection = mock(ServiceConnection.class);
        when(mockConnection.getEncryptedToken()).thenReturn("encrypted_token");

        User mockUser = mock(User.class);
        when(mockUser.activeConnectionFor("google_calendar")).thenReturn(Optional.of(mockConnection));
        when(userPersistencePort.findById(userId)).thenReturn(Optional.of(mockUser));

        when(encryptionService.decrypt("encrypted_token")).thenReturn("ya29.test-access-token");
    }
}
