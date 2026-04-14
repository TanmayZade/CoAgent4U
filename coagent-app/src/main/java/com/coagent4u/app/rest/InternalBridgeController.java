package com.coagent4u.app.rest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.agent.domain.Agent;
import com.coagent4u.agent.port.in.HandleMessageUseCase;
import com.coagent4u.agent.port.out.AgentPersistencePort;
import com.coagent4u.agent.port.out.CalendarPort;
import com.coagent4u.common.DomainEventPublisher;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CalendarEvent;
import com.coagent4u.shared.EventId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.out.NotificationPort;
import com.coagent4u.user.port.out.UserPersistencePort;
import com.coagent4u.security.AesTokenEncryption;
import com.coagent4u.config.CoagentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Internal API consumed ONLY by the Python agent service.
 * <p>
 * Not exposed externally — secured by network isolation (Docker internal network)
 * and the X-Internal-Service header check.
 * <p>
 * Each endpoint is a thin wrapper around existing ports/adapters.
 * No business logic lives here — it's a bridge, not a service.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalBridgeController {

    private static final Logger log = LoggerFactory.getLogger(InternalBridgeController.class);

    private final CalendarPort calendarPort;
    private final NotificationPort notificationPort;
    private final UserPersistencePort userPersistencePort;
    private final AgentPersistencePort agentPersistencePort;
    private final HandleMessageUseCase handleMessageUseCase;
    private final DomainEventPublisher eventPublisher;
    private final AesTokenEncryption encryptionService;
    private final CoagentProperties properties;
    private final ObjectMapper objectMapper;

    public InternalBridgeController(
            CalendarPort calendarPort,
            NotificationPort notificationPort,
            UserPersistencePort userPersistencePort,
            AgentPersistencePort agentPersistencePort,
            HandleMessageUseCase handleMessageUseCase,
            DomainEventPublisher eventPublisher,
            AesTokenEncryption encryptionService,
            CoagentProperties properties,
            ObjectMapper objectMapper) {
        this.calendarPort = calendarPort;
        this.notificationPort = notificationPort;
        this.userPersistencePort = userPersistencePort;
        this.agentPersistencePort = agentPersistencePort;
        this.handleMessageUseCase = handleMessageUseCase;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    // ──────────────────────────────────────────────────────────────
    // Calendar (wraps existing GoogleCalendarAdapter)
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/calendar/{agentId}/events")
    public ResponseEntity<?> getCalendarEvents(
            @PathVariable String agentId,
            @RequestParam String start,
            @RequestParam String end) {
        log.debug("[Bridge] GET /calendar/{}/events start={} end={}", agentId, start, end);
        try {
            AgentId aid = parseAgentId(agentId);
            TimeRange range = TimeRange.of(LocalDate.parse(start), LocalDate.parse(end));
            List<CalendarEvent> events = calendarPort.getCalendarEvents(aid, range);

            List<Map<String, String>> response = events.stream()
                    .map(e -> Map.of(
                            "event_id", e.eventId().value(),
                            "title", e.title(),
                            "start", e.slot().start().toString(),
                            "end", e.slot().end().toString()))
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[Bridge] Calendar events error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/calendar/{agentId}/freebusy")
    public ResponseEntity<?> getFreeBusy(
            @PathVariable String agentId,
            @RequestParam String start,
            @RequestParam String end) {
        log.debug("[Bridge] GET /calendar/{}/freebusy", agentId);
        try {
            AgentId aid = parseAgentId(agentId);
            TimeRange range = TimeRange.of(LocalDate.parse(start), LocalDate.parse(end));
            List<TimeSlot> slots = calendarPort.getFreeBusy(aid, range);

            List<Map<String, String>> response = slots.stream()
                    .map(s -> Map.of(
                            "start", s.start().toString(),
                            "end", s.end().toString()))
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[Bridge] FreeBusy error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/calendar/{agentId}/events")
    public ResponseEntity<?> createCalendarEvent(
            @PathVariable String agentId,
            @RequestBody CreateEventBridgeRequest request) {
        log.debug("[Bridge] POST /calendar/{}/events title={}", agentId, request.title());
        try {
            AgentId aid = parseAgentId(agentId);
            TimeSlot slot = new TimeSlot(
                    Instant.parse(request.start()),
                    Instant.parse(request.end()));
            EventId eventId = calendarPort.createEvent(aid, slot, request.title());
            return ResponseEntity.ok(Map.of("event_id", eventId.value()));
        } catch (Exception e) {
            log.error("[Bridge] Create event error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/calendar/{agentId}/events/{eventId}")
    public ResponseEntity<?> deleteCalendarEvent(
            @PathVariable String agentId,
            @PathVariable String eventId) {
        log.debug("[Bridge] DELETE /calendar/{}/events/{}", agentId, eventId);
        try {
            AgentId aid = parseAgentId(agentId);
            calendarPort.deleteEvent(aid, new EventId(eventId));
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            log.error("[Bridge] Delete event error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Token Persistence (via Java AES Encryption)
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/agent/{agentId}/google-token")
    public ResponseEntity<?> getGoogleToken(@PathVariable String agentId) {
        log.debug("[Bridge] GET /agent/{}/google-token", agentId);
        try {
            AgentId aid = parseAgentId(agentId);
            var agentOpt = agentPersistencePort.findById(aid);
            if (agentOpt.isEmpty()) return ResponseEntity.notFound().build();
            var userOpt = userPersistencePort.findById(agentOpt.get().getUserId());
            if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
            
            var connOpt = userOpt.get().activeConnectionFor("GOOGLE_CALENDAR");
            if (connOpt.isEmpty()) return ResponseEntity.notFound().build();
            
            var conn = connOpt.get();
            String decryptedToken = encryptionService.decrypt(conn.getEncryptedToken());
            
            // Detect format: new Python flow stores a full JSON blob,
            // old Java flow stored just the raw access token string
            if (decryptedToken != null && decryptedToken.trim().startsWith("{")) {
                // New format — already a full JSON credential blob
                return ResponseEntity.ok(Map.of("token_json", decryptedToken));
            }
            
            // Old format — raw access token. Build the full credential JSON.
            String decryptedRefresh = encryptionService.decrypt(conn.getEncryptedRefreshToken());
            ObjectNode tokenJson = objectMapper.createObjectNode();
            tokenJson.put("token", decryptedToken);
            if (decryptedRefresh != null && !"no_refresh_token".equals(decryptedRefresh)) {
                tokenJson.put("refresh_token", decryptedRefresh);
            }
            tokenJson.put("token_uri", "https://oauth2.googleapis.com/token");
            tokenJson.put("client_id", properties.getGoogle().getClientId());
            tokenJson.put("client_secret", properties.getGoogle().getClientSecret());
            if (conn.getTokenExpiresAt() != null) {
                tokenJson.put("expiry", conn.getTokenExpiresAt().toString());
            }
            
            return ResponseEntity.ok(Map.of("token_json", objectMapper.writeValueAsString(tokenJson)));
        } catch (Exception e) {
            log.error("[Bridge] Token GET error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/agent/{agentId}/google-token")
    public ResponseEntity<?> saveGoogleToken(@PathVariable String agentId, @RequestBody Map<String, String> payload) {
        log.debug("[Bridge] POST /agent/{}/google-token", agentId);
        try {
            AgentId aid = parseAgentId(agentId);
            var agentOpt = agentPersistencePort.findById(aid);
            if (agentOpt.isEmpty()) return ResponseEntity.notFound().build();
            var userOpt = userPersistencePort.findById(agentOpt.get().getUserId());
            if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
            
            User user = userOpt.get();
            String tokenJson = payload.get("token_json");
            String encryptedToken = encryptionService.encrypt(tokenJson);
            
            user.connectService("GOOGLE_CALENDAR", encryptedToken, "migrated", null);
            userPersistencePort.save(user);
            
            return ResponseEntity.ok(Map.of("status", "saved"));
        } catch (Exception e) {
            log.error("[Bridge] Token POST error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/agent/{agentId}/google-token")
    public ResponseEntity<?> deleteGoogleToken(@PathVariable String agentId) {
        log.debug("[Bridge] DELETE /agent/{}/google-token", agentId);
        try {
            AgentId aid = parseAgentId(agentId);
            var agentOpt = agentPersistencePort.findById(aid);
            if (agentOpt.isEmpty()) return ResponseEntity.notFound().build();
            var userOpt = userPersistencePort.findById(agentOpt.get().getUserId());
            if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
            
            User user = userOpt.get();
            user.disconnectService("GOOGLE_CALENDAR");
            userPersistencePort.save(user);
            
            return ResponseEntity.ok(Map.of("status", "deleted"));
        } catch (Exception e) {
            log.error("[Bridge] Token DELETE error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Notifications (wraps existing SlackNotificationAdapter)
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/notify")
    public ResponseEntity<?> sendNotification(@RequestBody NotifyBridgeRequest request) {
        log.debug("[Bridge] POST /notify userId={}", request.userId());
        try {
            UserId userId = new UserId(java.util.UUID.fromString(request.userId()));
            Optional<User> userOpt = userPersistencePort.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + request.userId()));
            }
            User user = userOpt.get();
            notificationPort.sendMessage(
                    user.getSlackIdentity().slackUserId(),
                    user.getSlackIdentity().workspaceId(),
                    request.message());
            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception e) {
            log.error("[Bridge] Notify error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Users (wraps existing UserPersistencePort)
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        log.debug("[Bridge] GET /user/{}", userId);
        try {
            UserId uid = new UserId(java.util.UUID.fromString(userId));
            Optional<User> userOpt = userPersistencePort.findById(uid);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "user_id", user.getUserId().value().toString(),
                    "username", user.getUsername(),
                    "slack_user_id", user.getSlackIdentity().slackUserId().value(),
                    "workspace_id", user.getSlackIdentity().workspaceId().value(),
                    "calendar_connected", user.activeConnectionFor("GOOGLE_CALENDAR").isPresent()));
        } catch (Exception e) {
            log.error("[Bridge] User lookup error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/agent/by-user/{userId}")
    public ResponseEntity<?> getAgentByUser(@PathVariable String userId) {
        log.debug("[Bridge] GET /agent/by-user/{}", userId);
        try {
            UserId uid = new UserId(java.util.UUID.fromString(userId));
            Optional<Agent> agentOpt = agentPersistencePort.findByUserId(uid);
            if (agentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Agent agent = agentOpt.get();
            return ResponseEntity.ok(Map.of(
                    "agent_id", agent.getAgentId().value().toString(),
                    "user_id", agent.getUserId().value().toString(),
                    "status", agent.getStatus().name()));
        } catch (Exception e) {
            log.error("[Bridge] Agent lookup error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Audit / Transparency (wraps existing monitoring module)
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/audit/log")
    public ResponseEntity<?> logAuditEntry(@RequestBody AuditLogBridgeRequest request) {
        log.debug("[Bridge] POST /audit/log agent={} action={}", request.agentId(), request.action());
        // In Phase 3, this will write enriched transparency entries.
        // For now, just log it.
        log.info("[Transparency] agent={} action={} details={}",
                request.agentId(), request.action(), request.details());
        return ResponseEntity.ok(Map.of("status", "logged"));
    }

    // ──────────────────────────────────────────────────────────────
    // Passthrough (for deterministic intents — Java handles them)
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/agent/passthrough")
    public ResponseEntity<?> passthroughToJava(@RequestBody PassthroughBridgeRequest request) {
        log.debug("[Bridge] POST /agent/passthrough agent={} intent={}",
                request.agentId(), request.intentType());
        try {
            AgentId agentId = parseAgentId(request.agentId());
            handleMessageUseCase.handleMessage(agentId, request.rawText());
            return ResponseEntity.ok(Map.of(
                    "status", "handled",
                    "message", "Intent processed by Java deterministic engine"));
        } catch (Exception e) {
            log.error("[Bridge] Passthrough error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Health
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<?> bridgeHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "coagent-java-bridge"));
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers + DTOs
    // ──────────────────────────────────────────────────────────────

    private AgentId parseAgentId(String raw) {
        return new AgentId(java.util.UUID.fromString(raw));
    }

    // Bridge DTOs
    public record CreateEventBridgeRequest(String title, String start, String end) {
    }

    public record NotifyBridgeRequest(String userId, String message, Map<String, Object> blocks) {
    }

    public record AuditLogBridgeRequest(String agentId, String action, Map<String, Object> details) {
    }

    public record PassthroughBridgeRequest(String agentId, String userId, String rawText,
            String intentType) {
    }
}
