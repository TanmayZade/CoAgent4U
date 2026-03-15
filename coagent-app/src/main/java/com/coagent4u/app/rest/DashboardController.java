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

import com.coagent4u.agent.application.dto.DashboardSummary;
import com.coagent4u.agent.port.in.GetDashboardSummaryUseCase;
import com.coagent4u.approval.application.dto.PendingApprovalDto;
import com.coagent4u.approval.port.in.GetPendingApprovalsUseCase;

/**
 * REST controller for the main dashboard view.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final GetDashboardSummaryUseCase dashboardSummaryUseCase;
    private final GetPendingApprovalsUseCase pendingApprovalsUseCase;

    public DashboardController(GetDashboardSummaryUseCase dashboardSummaryUseCase,
                                GetPendingApprovalsUseCase pendingApprovalsUseCase) {
        this.dashboardSummaryUseCase = dashboardSummaryUseCase;
        this.pendingApprovalsUseCase = pendingApprovalsUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String username) {
        try {
            DashboardSummary summary = dashboardSummaryUseCase.getSummary(username);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            log.warn("[DashboardController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<?> getPendingApprovals(@RequestParam String username) {
        try {
            List<PendingApprovalDto> approvals = pendingApprovalsUseCase.getPendingApprovals(username);
            return ResponseEntity.ok(approvals);
        } catch (IllegalArgumentException e) {
            log.warn("[DashboardController] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
