package com.coagent4u.app.rest;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.user.application.dto.AuditLogEntry;
import com.coagent4u.user.port.in.GetAuditLogUseCase;
import com.coagent4u.user.port.out.AuditLogQueryPort;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * REST controller for audit log page.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private final GetAuditLogUseCase auditLogUseCase;
    private final UserQueryPort userQuery;
    private final AuditLogQueryPort auditLogQuery;

    public AuditLogController(GetAuditLogUseCase auditLogUseCase,
                               UserQueryPort userQuery,
                               AuditLogQueryPort auditLogQuery) {
        this.auditLogUseCase = auditLogUseCase;
        this.userQuery = userQuery;
        this.auditLogQuery = auditLogQuery;
    }

    @GetMapping
    public ResponseEntity<?> getAuditLogs(
            @RequestParam String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<AuditLogEntry> response = auditLogUseCase.getAuditLog(
                    username, eventType, page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[AuditLogController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportAuditLogs(@RequestParam String username) {
        try {
            var user = userQuery.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

            List<AuditLogEntry> allLogs = auditLogQuery.findAllByUserId(user.getUserId());
            return ResponseEntity.ok(allLogs);
        } catch (IllegalArgumentException e) {
            log.warn("[AuditLogController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
