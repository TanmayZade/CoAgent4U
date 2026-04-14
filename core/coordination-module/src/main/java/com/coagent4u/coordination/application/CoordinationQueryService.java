package com.coagent4u.coordination.application;

import java.util.List;
import java.util.Optional;

import com.coagent4u.coordination.application.dto.CoordinationActivityPoint;
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
    public record AgentProfile(String username, String displayName, String avatarUrl) {}

    public interface UserAgentResolver {
        /** Returns the AgentId for the given username, or empty if user/agent not found. */
        java.util.Optional<AgentId> resolveAgentId(String username);

        /** Returns the AgentProfile for the given AgentId. */
        AgentProfile resolveProfile(AgentId agentId);

        /** Returns a map of AgentProfiles for the given AgentIds. */
        default java.util.Map<AgentId, AgentProfile> resolveProfiles(java.util.Collection<AgentId> agentIds) {
            java.util.Map<AgentId, AgentProfile> profiles = new java.util.HashMap<>();
            for (AgentId id : agentIds) {
                profiles.put(id, resolveProfile(id));
            }
            return profiles;
        }

        /** @deprecated Use {@link #resolveProfile(AgentId)} */
        @Deprecated
        String resolveUsername(AgentId agentId);
        /** @deprecated Use {@link #resolveProfile(AgentId)} */
        @Deprecated
        default String resolveDisplayName(AgentId agentId) { return null; }
        /** @deprecated Use {@link #resolveProfile(AgentId)} */
        @Deprecated
        default String resolveAvatarUrl(AgentId agentId) { return null; }
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

        // Batch resolve participants to avoid N+1 queries
        java.util.Set<AgentId> participantIds = coordinations.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getRequesterAgentId(), c.getInviteeAgentId()))
                .collect(java.util.stream.Collectors.toSet());
        
        java.util.Map<AgentId, AgentProfile> profiles = resolver.resolveProfiles(participantIds);

        List<CoordinationSummary> summaries = coordinations.stream()
                .map(c -> toSummary(c, agentId, profiles))
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
                .map(c -> toDetail(c, viewerAgentId));
    }

    public List<CoordinationActivityPoint> getActivityStats(AgentId agentId, int lastDays) {
        java.time.Instant since = java.time.Instant.now().minus(lastDays, java.time.temporal.ChronoUnit.DAYS);
        return persistence.findActivityStats(agentId, since);
    }

    private CoordinationSummary toSummary(Coordination c, AgentId viewerAgentId, java.util.Map<AgentId, AgentProfile> profiles) {
        // Determine the "other" participant's profile
        boolean isRequester = c.getRequesterAgentId().equals(viewerAgentId);
        AgentId otherAgentId = isRequester ? c.getInviteeAgentId() : c.getRequesterAgentId();
        
        AgentProfile profile = profiles.get(otherAgentId);
        String withUsername = profile != null ? profile.username() : "unknown";
        String withDisplayName = profile != null ? profile.displayName() : null;
        String withAvatarUrl = profile != null ? profile.avatarUrl() : null;
        String role = isRequester ? "REQUESTER" : "INVITEE";

        String title = c.getProposal() != null ? c.getProposal().title() : null;
        java.time.Instant time = c.getProposal() != null ? c.getProposal().suggestedTime().start() : null;

        return new CoordinationSummary(
                c.getCoordinationId().value(),
                withUsername,
                withDisplayName,
                withAvatarUrl,
                role,
                c.getState().name(),
                c.getCreatedAt(),
                title,
                time);
    }

    private CoordinationDetail toDetail(Coordination c, AgentId viewerAgentId) {
        boolean isRequester = c.getRequesterAgentId().equals(viewerAgentId);
        String role = isRequester ? "REQUESTER" : "INVITEE";

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

        AgentProfile requesterProfile = resolver.resolveProfile(c.getRequesterAgentId());
        AgentProfile inviteeProfile = resolver.resolveProfile(c.getInviteeAgentId());

        return new CoordinationDetail(
                c.getCoordinationId().value(),
                requesterProfile.username(),
                requesterProfile.displayName(),
                requesterProfile.avatarUrl(),
                inviteeProfile.username(),
                inviteeProfile.displayName(),
                inviteeProfile.avatarUrl(),
                role,
                c.getState().name(),
                proposalDto,
                c.getCreatedAt(),
                c.getCompletedAt(),
                logEntries);
    }
}
