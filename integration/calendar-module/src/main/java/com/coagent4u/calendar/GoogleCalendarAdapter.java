package com.coagent4u.calendar;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.common.exception.ExternalServiceUnavailableException;
import com.coagent4u.common.exception.TokenExpiredException;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.security.AesTokenEncryption;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CalendarEvent;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.ServiceConnection;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.OAuthTokenExchangePort;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Google Calendar API v3 adapter — implements {@link CalendarPort} and
 * {@link OAuthTokenExchangePort}.
 *
 * <p>
 * Error mapping:
 * </p>
 * <ul>
 * <li>Google 401 → {@link TokenExpiredException}</li>
 * <li>Google 429 / 5xx → {@link ExternalServiceUnavailableException}</li>
 * </ul>
 */
@Component
public class GoogleCalendarAdapter implements CalendarPort, OAuthTokenExchangePort {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);
    private static final String GOOGLE_CALENDAR_BASE = "https://www.googleapis.com/calendar/v3";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_INSTANT;

    private final WebClient webClient;
    private final WebClient tokenClient;
    private final CoagentProperties properties;
    private final AesTokenEncryption encryptionService;
    private final AgentPersistencePort agentPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final ObjectMapper objectMapper;

    public GoogleCalendarAdapter(
            WebClient.Builder webClientBuilder,
            CoagentProperties properties,
            AesTokenEncryption encryptionService,
            AgentPersistencePort agentPersistencePort,
            UserPersistencePort userPersistencePort,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(GOOGLE_CALENDAR_BASE).build();
        this.tokenClient = webClientBuilder.baseUrl(GOOGLE_TOKEN_URL).build();
        this.properties = properties;
        this.encryptionService = encryptionService;
        this.agentPersistencePort = agentPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.objectMapper = objectMapper;
    }

    // ── OAuthTokenExchangePort ──────────────────────────────────

    /**
     * Exchanges a Google OAuth authorization code for an access/refresh token pair.
     * Tokens are encrypted with AES-256-GCM before being returned.
     */
    @Override
    public OAuthTokenResult exchangeCode(String authorizationCode) {
        log.info("Exchanging OAuth authorization code for tokens");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", authorizationCode);
        formData.add("client_id", properties.getGoogle().getClientId());
        formData.add("client_secret", properties.getGoogle().getClientSecret());
        formData.add("redirect_uri", properties.getGoogle().getRedirectUri());
        formData.add("grant_type", "authorization_code");

        try {
            String response = tokenClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);

            String accessToken = json.path("access_token").asText();
            String refreshToken = json.path("refresh_token").asText(null);
            int expiresInSeconds = json.path("expires_in").asInt(3600);

            if (accessToken == null || accessToken.isBlank()) {
                throw new ExternalServiceUnavailableException("GoogleOAuth",
                        "Token exchange returned empty access_token");
            }

            // Encrypt tokens before returning
            String encryptedAccess = encryptionService.encrypt(accessToken);
            String encryptedRefresh = (refreshToken != null && !refreshToken.isBlank())
                    ? encryptionService.encrypt(refreshToken)
                    : encryptionService.encrypt("no_refresh_token");

            Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

            log.info("OAuth token exchange successful, access token expires in {}s", expiresInSeconds);
            return new OAuthTokenResult(encryptedAccess, encryptedRefresh, expiresAt);

        } catch (WebClientResponseException e) {
            log.warn("OAuth token exchange failed: HTTP {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ExternalServiceUnavailableException("GoogleOAuth",
                    "Token exchange failed: HTTP " + e.getStatusCode(), e);
        } catch (ExternalServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OAuth token exchange failed unexpectedly: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("GoogleOAuth",
                    "Token exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TimeSlot> getEvents(AgentId agentId, TimeRange range) {
        Optional<String> tokenOpt = resolveAccessTokenOptional(agentId);
        if (tokenOpt.isEmpty()) {
            log.warn("No Google Calendar token for agentId={} — returning empty event list (local testing mode)",
                    agentId);
            return java.util.Collections.emptyList();
        }
        String token = tokenOpt.get();
        Instant rangeStart = range.start().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = range.end().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/calendars/primary/events")
                            .queryParam("timeMin", formatRfc3339(rangeStart))
                            .queryParam("timeMax", formatRfc3339(rangeEnd))
                            .queryParam("singleEvents", true)
                            .queryParam("orderBy", "startTime")
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEvents(response);

        } catch (WebClientResponseException e) {
            throw mapGoogleError(e);
        }
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(AgentId agentId, TimeRange range) {
        Optional<String> tokenOpt = resolveAccessTokenOptional(agentId);
        if (tokenOpt.isEmpty()) {
            log.warn("No Google Calendar token for agentId={} — returning empty event list",
                    agentId);
            return java.util.Collections.emptyList();
        }
        String token = tokenOpt.get();
        Instant rangeStart = range.start().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = range.end().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/calendars/primary/events")
                            .queryParam("timeMin", formatRfc3339(rangeStart))
                            .queryParam("timeMax", formatRfc3339(rangeEnd))
                            .queryParam("singleEvents", true)
                            .queryParam("orderBy", "startTime")
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseCalendarEvents(response);

        } catch (WebClientResponseException e) {
            throw mapGoogleError(e);
        }
    }

    @Override
    public List<TimeSlot> getFreeBusy(AgentId agentId, TimeRange range) {
        Optional<String> tokenOpt = resolveAccessTokenOptional(agentId);
        if (tokenOpt.isEmpty()) {
            log.warn("No Google Calendar token for agentId={} — returning empty busy slots (local testing mode)",
                    agentId);
            return java.util.Collections.emptyList();
        }
        String token = tokenOpt.get();
        Instant rangeStart = range.start().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant rangeEnd = range.end().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("timeMin", formatRfc3339(rangeStart));
            body.put("timeMax", formatRfc3339(rangeEnd));
            ArrayNode items = objectMapper.createArrayNode();
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", "primary");
            items.add(item);
            body.set("items", items);

            String response = webClient.post()
                    .uri("/freeBusy")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseFreeBusy(response);

        } catch (WebClientResponseException e) {
            throw mapGoogleError(e);
        } catch (Exception e) {
            throw new ExternalServiceUnavailableException("GoogleCalendar",
                    "FreeBusy query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public EventId createEvent(AgentId agentId, TimeSlot timeSlot, String title) {
        String token = resolveAccessToken(agentId);
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("summary", title);
            ObjectNode start = objectMapper.createObjectNode();
            start.put("dateTime", formatRfc3339(timeSlot.start()));
            event.set("start", start);
            ObjectNode end = objectMapper.createObjectNode();
            end.put("dateTime", formatRfc3339(timeSlot.end()));
            event.set("end", end);

            String response = webClient.post()
                    .uri("/calendars/primary/events")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(event))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(response);
            String eventId = responseJson.path("id").asText();
            log.info("Created Google Calendar event={} for agent={}", eventId, agentId);
            return new EventId(eventId);

        } catch (WebClientResponseException e) {
            throw mapGoogleError(e);
        } catch (Exception e) {
            throw new ExternalServiceUnavailableException("GoogleCalendar",
                    "Create event failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(AgentId agentId, EventId eventId) {
        String token = resolveAccessToken(agentId);
        try {
            webClient.delete()
                    .uri("/calendars/primary/events/{eventId}", eventId.value())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Deleted Google Calendar event={} for agent={}", eventId, agentId);

        } catch (WebClientResponseException e) {
            throw mapGoogleError(e);
        }
    }

    // ── Internal helpers ──────────────────────────────────────

    /**
     * Resolves the decrypted access token for the agent's user.
     * If the token is expired or within 5 minutes of expiry, automatically
     * refreshes it using the stored refresh token.
     * Returns empty if the user has no active Google Calendar connection.
     */
    private Optional<String> resolveAccessTokenOptional(AgentId agentId) {
        var agent = agentPersistencePort.findById(agentId)
                .orElseThrow(() -> new IllegalStateException("Agent not found: " + agentId));
        UserId userId = agent.getUserId();
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for agent: " + agentId));
        Optional<ServiceConnection> connectionOpt = user.activeConnectionFor("GOOGLE_CALENDAR");
        if (connectionOpt.isEmpty()) {
            return Optional.empty();
        }

        ServiceConnection connection = connectionOpt.get();

        // Check if token is expired or about to expire (5-minute buffer)
        Instant now = Instant.now();
        Instant expiresAt = connection.getTokenExpiresAt();
        boolean needsRefresh = (expiresAt != null && now.plusSeconds(300).isAfter(expiresAt));

        if (needsRefresh) {
            log.info("Access token expired or near-expiry for agentId={}, refreshing...", agentId);
            String freshToken = refreshAccessToken(connection, user);
            return Optional.of(freshToken);
        }

        return Optional.of(encryptionService.decrypt(connection.getEncryptedToken()));
    }

    /**
     * Uses the stored refresh token to obtain a new access token from Google.
     * Updates the ServiceConnection and persists the user.
     *
     * @return the decrypted new access token
     */
    private String refreshAccessToken(ServiceConnection connection, User user) {
        String decryptedRefreshToken = encryptionService.decrypt(connection.getEncryptedRefreshToken());

        // Guard: if we never received a real refresh token
        if ("no_refresh_token".equals(decryptedRefreshToken)) {
            log.warn("No refresh token available for user={}, marking connection expired", user.getUserId());
            connection.markExpired();
            userPersistencePort.save(user);
            throw new TokenExpiredException("GoogleCalendar",
                    "No refresh token — user must re-authorize Google Calendar");
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", properties.getGoogle().getClientId());
            formData.add("client_secret", properties.getGoogle().getClientSecret());
            formData.add("refresh_token", decryptedRefreshToken);
            formData.add("grant_type", "refresh_token");

            String response = tokenClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);

            String newAccessToken = json.path("access_token").asText();
            int expiresInSeconds = json.path("expires_in").asInt(3600);
            // Google may return a new refresh token (rare, but handle it)
            String newRefreshToken = json.path("refresh_token").asText(null);

            if (newAccessToken == null || newAccessToken.isBlank()) {
                throw new ExternalServiceUnavailableException("GoogleOAuth",
                        "Token refresh returned empty access_token");
            }

            // Encrypt and update the connection
            String encryptedAccess = encryptionService.encrypt(newAccessToken);
            String encryptedRefresh = (newRefreshToken != null && !newRefreshToken.isBlank())
                    ? encryptionService.encrypt(newRefreshToken)
                    : connection.getEncryptedRefreshToken(); // keep existing refresh token
            Instant newExpiresAt = Instant.now().plusSeconds(expiresInSeconds);

            // Update domain object and persist
            connection.refreshToken(encryptedAccess, encryptedRefresh, newExpiresAt);
            userPersistencePort.save(user);

            log.info("Access token refreshed for userId={}, new expiry in {}s",
                    user.getUserId(), expiresInSeconds);
            return newAccessToken;

        } catch (WebClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("Token refresh failed: HTTP {} — {}", status, e.getResponseBodyAsString());

            // 400/401 from Google means the refresh token is revoked
            if (status == 400 || status == 401) {
                connection.markExpired();
                userPersistencePort.save(user);
                throw new TokenExpiredException("GoogleCalendar",
                        "Refresh token revoked — user must re-authorize Google Calendar", e);
            }

            throw new ExternalServiceUnavailableException("GoogleCalendar",
                    "Token refresh failed: HTTP " + status, e);
        } catch (TokenExpiredException | ExternalServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token refresh failed unexpectedly: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("GoogleCalendar",
                    "Token refresh failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the decrypted access token for the agent's user.
     */
    private String resolveAccessToken(AgentId agentId) {
        return resolveAccessTokenOptional(agentId)
                .orElseThrow(() -> new TokenExpiredException("GoogleCalendar",
                        "No active Google Calendar connection for agent " + agentId));
    }

    /**
     * Maps Google API HTTP errors to domain exceptions.
     */
    private RuntimeException mapGoogleError(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401) {
            return new TokenExpiredException("GoogleCalendar",
                    "Access token invalid or expired", e);
        }
        if (status == 429 || e.getStatusCode().is5xxServerError()) {
            return new ExternalServiceUnavailableException("GoogleCalendar",
                    "HTTP " + status + ": " + e.getResponseBodyAsString(), e);
        }
        return new ExternalServiceUnavailableException("GoogleCalendar",
                "HTTP " + status + ": " + e.getResponseBodyAsString(), e);
    }

    private List<TimeSlot> parseEvents(String json) {
        List<TimeSlot> slots = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            for (JsonNode item : items) {
                Instant start = parseDateTime(item.path("start"));
                Instant end = parseDateTime(item.path("end"));
                if (start != null && end != null) {
                    slots.add(new TimeSlot(start, end));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Google Calendar events response", e);
        }
        return slots;
    }

    private List<CalendarEvent> parseCalendarEvents(String json) {
        List<CalendarEvent> events = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("items");
            for (JsonNode item : items) {
                String id = item.path("id").asText();
                String title = item.path("summary").asText("(No title)");
                Instant start = parseDateTime(item.path("start"));
                Instant end = parseDateTime(item.path("end"));
                if (id != null && start != null && end != null) {
                    events.add(new CalendarEvent(new EventId(id), title, new TimeSlot(start, end)));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Google Calendar events response", e);
        }
        return events;
    }

    private List<TimeSlot> parseFreeBusy(String json) {
        List<TimeSlot> busySlots = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode busy = root.path("calendars").path("primary").path("busy");
            for (JsonNode slot : busy) {
                Instant start = Instant.parse(slot.path("start").asText());
                Instant end = Instant.parse(slot.path("end").asText());
                busySlots.add(new TimeSlot(start, end));
            }
        } catch (Exception e) {
            log.warn("Failed to parse Google FreeBusy response", e);
        }
        return busySlots;
    }

    private Instant parseDateTime(JsonNode dateTimeNode) {
        String dateTime = dateTimeNode.path("dateTime").asText(null);
        if (dateTime != null) {
            return Instant.parse(dateTime);
        }
        // All-day event fallback
        String date = dateTimeNode.path("date").asText(null);
        if (date != null) {
            return java.time.LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return null;
    }

    private String formatRfc3339(Instant instant) {
        return RFC3339.format(instant);
    }
}
