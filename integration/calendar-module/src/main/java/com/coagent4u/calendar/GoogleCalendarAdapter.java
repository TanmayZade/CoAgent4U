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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.CalendarPort;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Google Calendar API v3 adapter — implements {@link CalendarPort}.
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
public class GoogleCalendarAdapter implements CalendarPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);
    private static final String GOOGLE_CALENDAR_BASE = "https://www.googleapis.com/calendar/v3";
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_INSTANT;

    private final WebClient webClient;
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
        this.properties = properties;
        this.encryptionService = encryptionService;
        this.agentPersistencePort = agentPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TimeSlot> getEvents(AgentId agentId, TimeRange range) {
        String token = resolveAccessToken(agentId);
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
    public List<TimeSlot> getFreeBusy(AgentId agentId, TimeRange range) {
        String token = resolveAccessToken(agentId);
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
     */
    private String resolveAccessToken(AgentId agentId) {
        var agent = agentPersistencePort.findById(agentId)
                .orElseThrow(() -> new IllegalStateException("Agent not found: " + agentId));

        UserId userId = agent.getUserId();
        User user = userPersistencePort.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found for agent: " + agentId));

        Optional<ServiceConnection> connection = user.activeConnectionFor("google_calendar");
        if (connection.isEmpty()) {
            throw new TokenExpiredException("GoogleCalendar",
                    "No active Google Calendar connection for user " + userId);
        }

        return encryptionService.decrypt(connection.get().getEncryptedToken());
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
