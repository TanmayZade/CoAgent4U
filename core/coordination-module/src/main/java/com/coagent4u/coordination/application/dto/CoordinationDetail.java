package com.coagent4u.coordination.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed coordination data for the deep-dive dashboard view.
 */
public record CoordinationDetail(
        UUID coordinationId,
        String requesterUsername,
        String requesterDisplayName,
        String requesterAvatarUrl,
        String inviteeUsername,
        String inviteeDisplayName,
        String inviteeAvatarUrl,
        String role,
        String state,
        MeetingProposalDto proposal,
        Instant createdAt,
        Instant completedAt,
        List<StateLogEntryDto> stateLog
) {
    public record StateLogEntryDto(
            String fromState,
            String toState,
            String reason,
            Instant transitionedAt
    ) {}

    public record MeetingProposalDto(
            String title,
            Instant startTime,
            Instant endTime,
            int durationMinutes,
            String timeZone
    ) {}
}
