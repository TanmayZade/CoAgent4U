package com.coagent4u.app.rest;

import java.time.Instant;
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
import com.coagent4u.user.application.dto.AgentActivityEntry;
import com.coagent4u.user.port.in.GetAgentActivityUseCase;
import com.coagent4u.user.port.out.AgentActivityQueryPort;
import com.coagent4u.user.port.out.UserQueryPort;

/**
 * REST controller for agent activity page.
 */
@RestController
@RequestMapping("/api/agent-activities")
public class AgentActivityController {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityController.class);

    private final GetAgentActivityUseCase agentActivityUseCase;
    private final UserQueryPort userQuery;
    private final AgentActivityQueryPort agentActivityQuery;

    public AgentActivityController(GetAgentActivityUseCase agentActivityUseCase,
                               UserQueryPort userQuery,
                               AgentActivityQueryPort agentActivityQuery) {
        this.agentActivityUseCase = agentActivityUseCase;
        this.userQuery = userQuery;
        this.agentActivityQuery = agentActivityQuery;
    }

    @GetMapping
    public ResponseEntity<?> getAgentActivitys(
            @RequestParam String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PaginatedResponse<AgentActivityEntry> response = agentActivityUseCase.getAgentActivity(
                    username, eventType, level, startDate, endDate, page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[AgentActivityController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportAgentActivitys(@RequestParam String username) {
        try {
            var user = userQuery.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

            List<AgentActivityEntry> allLogs = agentActivityQuery.findAllByUserId(user.getUserId());
            return ResponseEntity.ok(allLogs);
        } catch (IllegalArgumentException e) {
            log.warn("[AgentActivityController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
