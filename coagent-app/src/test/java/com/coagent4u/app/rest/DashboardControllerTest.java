package com.coagent4u.app.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.coagent4u.agent.application.dto.DashboardSummary;
import com.coagent4u.agent.port.in.GetDashboardSummaryUseCase;
import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.coordination.port.in.GetCoordinationDetailUseCase;
import com.coagent4u.coordination.port.in.GetCoordinationHistoryUseCase;
import com.coagent4u.shared.CoordinationId;

/**
 * Standalone MockMvc tests for DashboardController and CoordinationController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard & Coordination Controller Tests")
class DashboardControllerTest {

    @Mock
    private GetDashboardSummaryUseCase dashboardSummaryUseCase;

    @Mock
    private com.coagent4u.approval.port.in.GetPendingApprovalsUseCase pendingApprovalsUseCase;

    @Mock
    private GetCoordinationHistoryUseCase historyUseCase;

    @Mock
    private GetCoordinationDetailUseCase detailUseCase;

    private MockMvc dashboardMvc;
    private MockMvc coordinationMvc;

    @BeforeEach
    void setUp() {
        dashboardMvc = MockMvcBuilders.standaloneSetup(
                new DashboardController(dashboardSummaryUseCase, pendingApprovalsUseCase)).build();
        coordinationMvc = MockMvcBuilders.standaloneSetup(
                new CoordinationController(historyUseCase, detailUseCase)).build();
    }

    // ── Dashboard Tests ──

    @Test
    @DisplayName("GET /api/dashboard/summary returns summary for valid user")
    void dashboardSummary_validUser() throws Exception {
        DashboardSummary summary = new DashboardSummary(
                new DashboardSummary.AgentStatusDto("ACTIVE", true, true, Instant.now()),
                2,
                List.of(new CoordinationSummary(UUID.randomUUID(), "bob", null, null, "REQUESTER", "INITIATED", Instant.now(), null, null)),
                List.of());

        when(dashboardSummaryUseCase.getSummary("alice")).thenReturn(summary);

        dashboardMvc.perform(get("/api/dashboard/summary").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentStatus.status").value("ACTIVE"))
                .andExpect(jsonPath("$.agentStatus.slackConnected").value(true))
                .andExpect(jsonPath("$.agentStatus.googleCalendarConnected").value(true))
                .andExpect(jsonPath("$.pendingApprovalsCount").value(2))
                .andExpect(jsonPath("$.recentCoordinations").isArray())
                .andExpect(jsonPath("$.recentCoordinations[0].withUsername").value("bob"));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary returns 400 for unknown user")
    void dashboardSummary_unknownUser() throws Exception {
        when(dashboardSummaryUseCase.getSummary("unknown"))
                .thenThrow(new IllegalArgumentException("Unknown user: unknown"));

        dashboardMvc.perform(get("/api/dashboard/summary").param("username", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown user: unknown"));
    }

    // ── Coordination History Tests ──

    @Test
    @DisplayName("GET /api/coordinations returns paginated history")
    void coordinationHistory_validUser() throws Exception {
        PaginatedResponse<CoordinationSummary> response = new PaginatedResponse<>(
                List.of(new CoordinationSummary(UUID.randomUUID(), "bob", null, null, "REQUESTER", "COMPLETED", Instant.now(), null, null)),
                0, 10, 1, 1);

        when(historyUseCase.getHistory("alice", null, 0, 10)).thenReturn(response);

        coordinationMvc.perform(get("/api/coordinations")
                        .param("username", "alice")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].withUsername").value("bob"))
                .andExpect(jsonPath("$.content[0].state").value("COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/coordinations returns 400 for unknown user")
    void coordinationHistory_unknownUser() throws Exception {
        when(historyUseCase.getHistory("unknown", null, 0, 10))
                .thenThrow(new IllegalArgumentException("Unknown user: unknown"));

        coordinationMvc.perform(get("/api/coordinations")
                        .param("username", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown user: unknown"));
    }

    // ── Coordination Detail Tests ──

    @Test
    @DisplayName("GET /api/coordinations/{id} returns detail for authorized user")
    void coordinationDetail_authorized() throws Exception {
        UUID coordId = UUID.randomUUID();
        CoordinationDetail detail = new CoordinationDetail(
                coordId, "alice", "bob", "COMPLETED", null,
                Instant.now(), Instant.now(),
                List.of(new CoordinationDetail.StateLogEntryDto(null, "INITIATED", "Start", Instant.now())));

        when(detailUseCase.getDetail(new CoordinationId(coordId), "alice"))
                .thenReturn(Optional.of(detail));

        coordinationMvc.perform(get("/api/coordinations/" + coordId)
                        .param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterUsername").value("alice"))
                .andExpect(jsonPath("$.inviteeUsername").value("bob"))
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.stateLog").isArray());
    }

    @Test
    @DisplayName("GET /api/coordinations/{id} returns 403 for unauthorized user")
    void coordinationDetail_unauthorized() throws Exception {
        UUID coordId = UUID.randomUUID();

        when(detailUseCase.getDetail(new CoordinationId(coordId), "charlie"))
                .thenReturn(Optional.empty());

        coordinationMvc.perform(get("/api/coordinations/" + coordId)
                        .param("username", "charlie"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Coordination not found or not authorized"));
    }
}
