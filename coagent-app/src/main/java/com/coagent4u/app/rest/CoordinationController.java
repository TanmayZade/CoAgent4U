package com.coagent4u.app.rest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.coordination.application.CoordinationQueryService;
import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.coordination.port.in.CoordinationProtocolPort;
import com.coagent4u.coordination.port.in.GetCoordinationDetailUseCase;
import com.coagent4u.coordination.port.in.GetCoordinationHistoryUseCase;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.shared.SlackUserId;
import com.coagent4u.shared.WorkspaceId;
import com.coagent4u.user.port.out.NotificationPort;

/**
 * REST controller for coordination history, detail views, and actions.
 */
@RestController
@RequestMapping("/api/coordinations")
public class CoordinationController {

    private static final Logger log = LoggerFactory.getLogger(CoordinationController.class);

    private final GetCoordinationHistoryUseCase historyUseCase;
    private final GetCoordinationDetailUseCase detailUseCase;
    private final CoordinationProtocolPort protocolPort;
    private final CoordinationQueryService.UserAgentResolver resolver;
    private final NotificationPort notificationPort;

    public CoordinationController(GetCoordinationHistoryUseCase historyUseCase,
                                   GetCoordinationDetailUseCase detailUseCase,
                                   CoordinationProtocolPort protocolPort,
                                   CoordinationQueryService.UserAgentResolver resolver,
                                   NotificationPort notificationPort) {
        this.historyUseCase = historyUseCase;
        this.detailUseCase = detailUseCase;
        this.protocolPort = protocolPort;
        this.resolver = resolver;
        this.notificationPort = notificationPort;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getHistory(
            @RequestParam String username,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PaginatedResponse<CoordinationSummary> response = historyUseCase.getHistory(username, status, page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(
            @PathVariable UUID id,
            @RequestParam String username) {
        try {
            Optional<CoordinationDetail> detail = detailUseCase.getDetail(new CoordinationId(id), username);
            return detail
                    .map(d -> ResponseEntity.ok((Object) d))
                    .orElseGet(() -> ResponseEntity.status(403)
                            .body(Map.of("error", "Coordination not found or not authorized")));
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<?> getAvailableSlots(
            @PathVariable UUID id,
            @RequestParam String username) {
        try {
            resolver.resolveAgentId(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            List<com.coagent4u.shared.TimeSlot> slots = protocolPort.getAvailableSlots(new CoordinationId(id));
            List<Map<String, Object>> result = slots.stream()
                    .map(s -> Map.<String, Object>of("start", s.start().toString(), "end", s.end().toString()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Get slots failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[CoordinationController] Get slots error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelCoordination(
            @PathVariable UUID id,
            @RequestParam String username) {
        try {
            resolver.resolveAgentId(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            CoordinationId coordId = new CoordinationId(id);
            // Delete all dangling Slack messages before terminating
            deleteSlackMessage(coordId, "slot_selection_ts",         "invitee_slack_id");
            deleteSlackMessage(coordId, "requester_notification_ts", "requester_slack_id");
            deleteSlackMessage(coordId, "selected_slot_ts",          "invitee_slack_id");
            deleteSlackMessage(coordId, "requester_approval_ts",      "requester_slack_id");
            protocolPort.terminate(coordId, "Cancelled by user " + username);
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Cancel failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[CoordinationController] Cancel error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Cancel failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveCoordination(
            @PathVariable UUID id,
            @RequestParam String username,
            @RequestParam boolean approved) {
        try {
            AgentId agentId = resolver.resolveAgentId(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            CoordinationId coordId = new CoordinationId(id);
            // Delete both participants' Slack approval/slot cards (safe — missing ts is a no-op)
            deleteSlackMessage(coordId, "selected_slot_ts",          "invitee_slack_id");
            deleteSlackMessage(coordId, "requester_notification_ts", "requester_slack_id");
            deleteSlackMessage(coordId, "requester_approval_ts",      "requester_slack_id");
            protocolPort.handleApproval(coordId, agentId, approved);
            return ResponseEntity.ok(Map.of("status", approved ? "approved" : "rejected"));
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Approval failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[CoordinationController] Approval error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Approval failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/select-slot")
    public ResponseEntity<?> selectSlot(
            @PathVariable UUID id,
            @RequestParam String username,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            resolver.resolveAgentId(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));
            CoordinationId coordId = new CoordinationId(id);
            // Delete invitee's slot picker card and requester's "here are the slots" preview
            deleteSlackMessage(coordId, "slot_selection_ts",         "invitee_slack_id");
            deleteSlackMessage(coordId, "requester_notification_ts", "requester_slack_id");

            com.coagent4u.shared.TimeSlot slot = new com.coagent4u.shared.TimeSlot(
                    Instant.parse(start), Instant.parse(end));

            // Post a status card to the invitee on Slack (Simulate "Selected Time Slot")
            try {
                String inviteeSlackId = protocolPort.getMetadata(coordId, "invitee_slack_id");
                String workspaceId = protocolPort.getMetadata(coordId, "workspace_id");
                if (inviteeSlackId != null && workspaceId != null) {
                    java.time.ZonedDateTime startZdt = slot.start().atZone(java.time.ZoneId.of("Asia/Kolkata"));
                    java.time.ZonedDateTime endZdt = slot.end().atZone(java.time.ZoneId.of("Asia/Kolkata"));
                    java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
                    java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");

                    String statusText = "🕐 *Selected Time Slot*\n\n"
                            + "📅 " + startZdt.format(dateFmt) + "\n"
                            + "🕐 " + startZdt.format(timeFmt) + " – " + endZdt.format(timeFmt) + "\n\n"
                            + "_Selected via Web_\n"
                            + "⏳ Waiting for approval...";

                    String newTs = notificationPort.sendStatusCard(
                            new SlackUserId(inviteeSlackId), new WorkspaceId(workspaceId), statusText, "#3AA3E3");
                    if (newTs != null) {
                        protocolPort.updateMetadata(coordId, "selected_slot_ts", newTs);
                    }
                }
            } catch (Exception e) {
                log.warn("[CoordinationController] Failed to post status card for invitee: {}", e.getMessage());
            }

            protocolPort.selectSlot(coordId, slot);
            return ResponseEntity.ok(Map.of("status", "slot_selected"));
        } catch (IllegalArgumentException e) {
            log.warn("[CoordinationController] Select slot failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[CoordinationController] Select slot error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Slack Cleanup Helper ──────────────────────────────────────────────────

    /**
     * Deletes a Slack message using timestamps stored in coordination metadata.
     *
     * @param coordId     the coordination whose metadata to read
     * @param tsKey       metadata key holding the Slack message timestamp (e.g. "slot_selection_ts")
     * @param slackIdKey  metadata key holding the Slack user ID (channel) for that message
     */
    private void deleteSlackMessage(CoordinationId coordId, String tsKey, String slackIdKey) {
        try {
            String ts = protocolPort.getMetadata(coordId, tsKey);
            String slackId = protocolPort.getMetadata(coordId, slackIdKey);
            String workspaceId = protocolPort.getMetadata(coordId, "workspace_id");
            if (ts != null && !ts.isBlank() && slackId != null && workspaceId != null) {
                boolean deleted = notificationPort.deleteMessage(
                        new SlackUserId(slackId), new WorkspaceId(workspaceId), ts);
                log.info("[CoordinationController] Deleted Slack message ts={} for slackId={}: {}", ts, slackId, deleted);
            }
        } catch (Exception e) {
            // Non-fatal — web action should still proceed even if Slack cleanup fails
            log.warn("[CoordinationController] Failed to delete Slack message ({}={}): {}", tsKey, slackIdKey, e.getMessage());
        }
    }
}
