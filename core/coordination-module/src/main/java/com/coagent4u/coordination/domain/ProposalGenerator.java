package com.coagent4u.coordination.domain;

import java.util.UUID;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.TimeSlot;

/**
 * Domain service that builds a {@link MeetingProposal} from a matched
 * {@link TimeSlot}.
 */
public class ProposalGenerator {

    /**
     * Generates a MeetingProposal from the matched time slot and participant
     * context.
     *
     * @param coordinationId  the coordination this proposal belongs to
     * @param requester       requester agent
     * @param invitee         invitee agent
     * @param slot            the matched time slot
     * @param durationMinutes planned meeting duration in minutes
     * @param title           meeting title (derived from coordination context)
     * @param timezone        timezone string (e.g. "Asia/Kolkata")
     */
    public MeetingProposal generate(String coordinationId,
            AgentId requester, AgentId invitee,
            TimeSlot slot, int durationMinutes,
            String title, String timezone) {
        String proposalId = "proposal-" + UUID.randomUUID();
        return new MeetingProposal(proposalId, requester, invitee,
                slot, durationMinutes, title, timezone);
    }
}
