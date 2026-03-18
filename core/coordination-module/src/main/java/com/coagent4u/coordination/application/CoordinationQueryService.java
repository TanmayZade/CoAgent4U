package com.coagent4u.coordination.application;

import java.util.List;
import java.util.Optional;

import com.coagent4u.coordination.application.dto.CoordinationDetail;
import com.coagent4u.coordination.application.dto.CoordinationSummary;
import com.coagent4u.coordination.domain.Coordination;
import com.coagent4u.coordination.port.in.GetCoordinationDetailUseCase;
import com.coagent4u.coordination.port.in.GetCoordinationHistoryUseCase;
import com.coagent4u.coordination.port.out.CoordinationPersistencePort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.PaginatedResponse;

/**
 * Read-only query service for coordination data.
 * Separated from {@link CoordinationService} (CQRS-lite: read path).
 *
 * <p>No Spring annotations — assembled by DI in coagent-app.
 */
public class CoordinationQueryService implements GetCoordinationHistoryUseCase, GetCoordinationDetailUseCase {

    /**
     * Functional interface so this service doesn't import user-module or agent-module.
     * Resolved at wiring time in coagent-app.
     */
    public interface UserAgentResolver {
        /** Returns the AgentId for the given username, or empty if user/agent not found. */
        Optional<AgentId> resolveAgentId(String username);
        /** Returns the username for the given AgentId, or "unknown". */
        String resolveUsername(AgentId agentId);
    }

    private final CoordinationPersistencePort persistence;
    private final UserAgentResolver resolver;

    public CoordinationQueryService(CoordinationPersistencePort persistence, UserAgentResolver resolver) {
        this.persistence = persistence;
        this.resolver = resolver;
    }

    @Override
    public PaginatedResponse<CoordinationSummary> getHistory(String username, String status, int page, int size) {
        AgentId agentId = resolver.resolveAgentId(username)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + username));

        int offset = page * size;
        
        List<Coordination> coordinations;
        long total;
        
        if (status == null || status.equalsIgnoreCase("ALL")) {
            coordinations = persistence.findByAgentId(agentId, offset, size);
            total = persistence.countByAgentId(agentId);
        } else if (status.equalsIgnoreCase("COMPLETED")) {
            List<com.coagent4u.coordination.domain.CoordinationState> states = List.of(com.coagent4u.coordination.domain.CoordinationState.COMPLETED);
            coordinations = persistence.findByAgentIdAndStates(agentId, states, offset, size);
            total = persistence.countByAgentIdAndStates(agentId, states);
        } else if (status.equalsIgnoreCase("PENDING")) {
            List<com.coagent4u.coordination.domain.CoordinationState> states = java.util.Arrays.stream(com.coagent4u.coordination.domain.CoordinationState.values())
                    .filter(s -> !s.isTerminal())
                    .toList();
            coordinations = persistence.findByAgentIdAndStates(agentId, states, offset, size);
            total = persistence.countByAgentIdAndStates(agentId, states);
        } else {
            throw new IllegalArgumentException("Invalid status filter: " + status);
        }

        List<CoordinationSummary> summaries = coordinations.stream()
                .map(c -> toSummary(c, agentId))
                .toList();

        return new PaginatedResponse<>(summaries, page, size, total,
                (int) Math.ceil((double) total / size));
    }

    @Override
    public Optional<CoordinationDetail> getDetail(CoordinationId id, String viewerUsername) {
        AgentId viewerAgentId = resolver.resolveAgentId(viewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + viewerUsername));

        return persistence.findById(id)
                .filter(c -> c.getRequesterAgentId().equals(viewerAgentId)
                        || c.getInviteeAgentId().equals(viewerAgentId))
                .map(this::toDetail);
    }

    private CoordinationSummary toSummary(Coordination c, AgentId viewerAgentId) {
        // Determine the "other" participant's username
        AgentId otherAgentId = c.getRequesterAgentId().equals(viewerAgentId)
                ? c.getInviteeAgentId()
                : c.getRequesterAgentId();
        String withUsername = resolver.resolveUsername(otherAgentId);

        String title = c.getProposal() != null ? c.getProposal().title() : null;
        java.time.Instant time = c.getProposal() != null ? c.getProposal().suggestedTime().start() : null;

        return new CoordinationSummary(
                c.getCoordinationId().value(),
                withUsername,
                c.getState().name(),
                c.getCreatedAt(),
                title,
                time);
    }

    private CoordinationDetail toDetail(Coordination c) {
        List<CoordinationDetail.StateLogEntryDto> logEntries = c.getStateLog().stream()
                .map(entry -> new CoordinationDetail.StateLogEntryDto(
                        entry.fromState() != null ? entry.fromState().name() : null,
                        entry.toState().name(),
                        entry.reason(),
                        entry.timestamp()))
                .toList();

        CoordinationDetail.MeetingProposalDto proposalDto = null;
        if (c.getProposal() != null) {
            proposalDto = new CoordinationDetail.MeetingProposalDto(
                    c.getProposal().title(),
                    c.getProposal().suggestedTime().start(),
                    c.getProposal().suggestedTime().end(),
                    c.getProposal().durationMinutes(),
                    c.getProposal().timezone());
        }

        return new CoordinationDetail(
                c.getCoordinationId().value(),
                resolver.resolveUsername(c.getRequesterAgentId()),
                resolver.resolveUsername(c.getInviteeAgentId()),
                c.getState().name(),
                proposalDto,
                c.getCreatedAt(),
                c.getCompletedAt(),
                logEntries);
    }
}
