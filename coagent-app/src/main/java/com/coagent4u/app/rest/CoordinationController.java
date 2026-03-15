package com.coagent4u.app.rest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.coordination.port.in.GetCoordinationDetailUseCase;
import com.coagent4u.coordination.port.in.GetCoordinationHistoryUseCase;
import com.coagent4u.shared.CoordinationId;

/**
 * REST controller for coordination history and detail views.
 */
@RestController
@RequestMapping("/api/coordinations")
public class CoordinationController {

    private static final Logger log = LoggerFactory.getLogger(CoordinationController.class);

    private final GetCoordinationHistoryUseCase historyUseCase;
    private final GetCoordinationDetailUseCase detailUseCase;

    public CoordinationController(GetCoordinationHistoryUseCase historyUseCase,
                                   GetCoordinationDetailUseCase detailUseCase) {
        this.historyUseCase = historyUseCase;
        this.detailUseCase = detailUseCase;
    }

    @GetMapping
    public ResponseEntity<?> getHistory(
            @RequestParam String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PaginatedResponse<CoordinationSummary> response = historyUseCase.getHistory(username, page, size);
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
}
