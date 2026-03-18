package com.coagent4u.coordination.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.shared.PaginatedResponse;
import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoordinationQueryService Tests")
class CoordinationQueryServiceTest {

    @Mock
    private CoordinationPersistencePort persistence;

    @Mock
    private CoordinationQueryService.UserAgentResolver resolver;

    private CoordinationQueryService service;

    @BeforeEach
    void setUp() {
        service = new CoordinationQueryService(persistence, resolver);
    }

    // ── getHistory ──

    @Test
    @DisplayName("getHistory returns paginated summaries for a valid user")
    void getHistory_validUser_returnsPaginatedSummaries() {
        String username = "alice";
        AgentId agentId = AgentId.generate();
        AgentId otherAgentId = AgentId.generate();

        when(resolver.resolveAgentId(username)).thenReturn(Optional.of(agentId));
        when(resolver.resolveUsername(otherAgentId)).thenReturn("bob");

        Coordination coord = new Coordination(CoordinationId.generate(), agentId, otherAgentId);
        when(persistence.findByAgentId(agentId, 0, 10)).thenReturn(List.of(coord));
        when(persistence.countByAgentId(agentId)).thenReturn(1L);

        PaginatedResponse<CoordinationSummary> result = service.getHistory(username, null, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals("bob", result.content().get(0).withUsername());
        assertEquals(CoordinationState.INITIATED.name(), result.content().get(0).state());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
    }

    @Test
    @DisplayName("getHistory throws for unknown user")
    void getHistory_unknownUser_throws() {
        when(resolver.resolveAgentId("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.getHistory("unknown", null, 0, 10));
    }

    @Test
    @DisplayName("getHistory returns empty page when no coordinations exist")
    void getHistory_noCoordinations_emptyPage() {
        String username = "alice";
        AgentId agentId = AgentId.generate();

        when(resolver.resolveAgentId(username)).thenReturn(Optional.of(agentId));
        when(persistence.findByAgentId(agentId, 0, 10)).thenReturn(List.of());
        when(persistence.countByAgentId(agentId)).thenReturn(0L);

        PaginatedResponse<CoordinationSummary> result = service.getHistory(username, null, 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    // ── getDetail ──

    @Test
    @DisplayName("getDetail returns detail for coordination where user is requester")
    void getDetail_asRequester_returnsDetail() {
        AgentId requester = AgentId.generate();
        AgentId invitee = AgentId.generate();
        CoordinationId coordId = CoordinationId.generate();
        Coordination coord = new Coordination(coordId, requester, invitee);

        when(resolver.resolveAgentId("alice")).thenReturn(Optional.of(requester));
        when(resolver.resolveUsername(requester)).thenReturn("alice");
        when(resolver.resolveUsername(invitee)).thenReturn("bob");
        when(persistence.findById(coordId)).thenReturn(Optional.of(coord));

        Optional<CoordinationDetail> result = service.getDetail(coordId, "alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().requesterUsername());
        assertEquals("bob", result.get().inviteeUsername());
        assertEquals(CoordinationState.INITIATED.name(), result.get().state());
        assertFalse(result.get().stateLog().isEmpty());
    }

    @Test
    @DisplayName("getDetail returns empty for coordination owned by another user")
    void getDetail_notParticipant_returnsEmpty() {
        AgentId requester = AgentId.generate();
        AgentId invitee = AgentId.generate();
        AgentId outsider = AgentId.generate();
        CoordinationId coordId = CoordinationId.generate();

        Coordination coord = new Coordination(coordId, requester, invitee);

        when(resolver.resolveAgentId("charlie")).thenReturn(Optional.of(outsider));
        when(persistence.findById(coordId)).thenReturn(Optional.of(coord));

        Optional<CoordinationDetail> result = service.getDetail(coordId, "charlie");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getDetail returns empty for non-existent coordination")
    void getDetail_notFound_returnsEmpty() {
        CoordinationId coordId = CoordinationId.generate();

        when(resolver.resolveAgentId("alice")).thenReturn(Optional.of(AgentId.generate()));
        when(persistence.findById(coordId)).thenReturn(Optional.empty());

        Optional<CoordinationDetail> result = service.getDetail(coordId, "alice");

        assertTrue(result.isEmpty());
    }
}
