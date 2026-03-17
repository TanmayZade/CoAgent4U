package com.coagent4u.coordination.domain;

import com.coagent4u.coordination.port.out.AgentEventExecutionPort;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.EventId;

/**
 * Domain service orchestrating the two-step event creation saga:
 * CREATING_EVENT_A → CREATING_EVENT_B → COMPLETED
 *
 * <p>
 * On failure after event A is created, compensates by deleting event A
 * before transitioning to FAILED.
 */
public class EventCreationSaga {

    public record SagaResult(boolean success, EventId eventIdA, EventId eventIdB) {}

    /**
     * Executes the saga for a given coordination.
     * Mutates the coordination state directly.
     *
     * @param coordination   the coordination in APPROVED_BY_BOTH state
     * @param agentEventExec outbound port to create/delete calendar events
     * @return SagaResult containing success status and created event IDs
     */
    public SagaResult execute(Coordination coordination, AgentEventExecutionPort agentEventExec) {
        MeetingProposal proposal = coordination.getProposal();
        AgentId requesterAgentId = coordination.getRequesterAgentId();
        AgentId inviteeAgentId = coordination.getInviteeAgentId();

        // Step 1: Create event for requester (Agent A)
        coordination.transition(CoordinationState.CREATING_EVENT_A, "Creating calendar event for requester");
        EventId eventIdA;
        try {
            eventIdA = agentEventExec.createEvent(requesterAgentId,
                    proposal.suggestedTime(), proposal.title());
        } catch (Exception e) {
            coordination.transition(CoordinationState.FAILED, "Failed to create event A: " + e.getMessage());
            return new SagaResult(false, null, null);
        }

        // Step 2: Create event for invitee (Agent B)
        coordination.transition(CoordinationState.CREATING_EVENT_B, "Creating calendar event for invitee");
        EventId eventIdB;
        try {
            eventIdB = agentEventExec.createEvent(inviteeAgentId, proposal.suggestedTime(), proposal.title());
        } catch (Exception e) {
            // Compensate: delete event A
            try {
                agentEventExec.deleteEvent(requesterAgentId, eventIdA);
            } catch (Exception compensationEx) {
                // Log compensation failure but still fail the coordination
            }
            coordination.transition(CoordinationState.FAILED,
                    "Failed to create event B (compensated A): " + e.getMessage());
            return new SagaResult(false, null, null);
        }

        coordination.transition(CoordinationState.COMPLETED, "Both calendar events created successfully");
        return new SagaResult(true, eventIdA, eventIdB);
    }
}
