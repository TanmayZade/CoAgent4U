package com.coagent4u.coordination.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeSlot;

@DisplayName("ProposalGenerator Tests")
class ProposalGeneratorTest {

    private final ProposalGenerator generator = new ProposalGenerator();

    @Test
    @DisplayName("generates proposal with correct fields")
    void generates_proposal_with_correct_fields() {
        AgentId requester = AgentId.generate();
        AgentId invitee = AgentId.generate();
        TimeSlot slot = new TimeSlot(
                Instant.parse("2026-03-05T10:00:00Z"),
                Instant.parse("2026-03-05T10:30:00Z"));
        String coordId = CoordinationId.generate().toString();

        MeetingProposal result = generator.generate(
                coordId, requester, invitee, slot, 30, "Team Sync", "Asia/Kolkata");

        assertNotNull(result.proposalId());
        assertTrue(result.proposalId().startsWith("proposal-"));
        assertEquals(requester, result.requesterAgentId());
        assertEquals(invitee, result.inviteeAgentId());
        assertEquals(slot, result.suggestedTime());
        assertEquals(30, result.durationMinutes());
        assertEquals("Team Sync", result.title());
        assertEquals("Asia/Kolkata", result.timezone());
    }

    @Test
    @DisplayName("proposal IDs are unique across invocations")
    void proposal_ids_are_unique() {
        AgentId requester = AgentId.generate();
        AgentId invitee = AgentId.generate();
        TimeSlot slot = new TimeSlot(
                Instant.parse("2026-03-05T10:00:00Z"),
                Instant.parse("2026-03-05T10:30:00Z"));
        String coordId = CoordinationId.generate().toString();

        MeetingProposal p1 = generator.generate(coordId, requester, invitee, slot, 30, "A", "UTC");
        MeetingProposal p2 = generator.generate(coordId, requester, invitee, slot, 30, "B", "UTC");

        assertNotEquals(p1.proposalId(), p2.proposalId());
    }
}
