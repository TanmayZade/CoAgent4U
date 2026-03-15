package com.coagent4u.app.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.user.port.in.DeleteUserUseCase;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * REST controller for account management (GDPR export, deletion).
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final UserQueryPort userQuery;
    private final DeleteUserUseCase deleteUserUseCase;
    private final com.coagent4u.user.port.out.AuditLogQueryPort auditLogQuery;

    public AccountController(UserQueryPort userQuery,
                              DeleteUserUseCase deleteUserUseCase,
                              com.coagent4u.user.port.out.AuditLogQueryPort auditLogQuery) {
        this.userQuery = userQuery;
        this.deleteUserUseCase = deleteUserUseCase;
        this.auditLogQuery = auditLogQuery;
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportUserData(@RequestParam String username) {
        try {
            var user = userQuery.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

            // Build a GDPR data export bundle
            var auditLogs = auditLogQuery.findAllByUserId(user.getUserId());

            Map<String, Object> exportBundle = Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail() != null ? user.getEmail().value() : "",
                    "createdAt", user.getCreatedAt().toString(),
                    "serviceConnections", user.getServiceConnections().stream()
                            .map(sc -> Map.of(
                                    "serviceType", sc.getServiceType(),
                                    "status", sc.getStatus().name(),
                                    "connectedAt", sc.getConnectedAt().toString()))
                            .toList(),
                    "auditLogs", auditLogs);

            return ResponseEntity.ok(exportBundle);
        } catch (IllegalArgumentException e) {
            log.warn("[AccountController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@RequestParam String username) {
        try {
            var user = userQuery.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

            deleteUserUseCase.delete(user.getUserId());
            log.info("[AccountController] Account deleted for user: {}", username);
            return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
        } catch (IllegalArgumentException e) {
            log.warn("[AccountController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("[AccountController] Conflict: {}", e.getMessage());
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
