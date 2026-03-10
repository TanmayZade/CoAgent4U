package com.coagent4u.coordination.port.in;

import java.util.List;

import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeRange;
import com.coagent4u.shared.TimeSlot;

/**
 * Inbound port — the primary API for managing a collaboration session.
 * Invoked exclusively by agent-module's {@code AgentCommandService}.
 *
 * <p>
 * Implemented by {@code CoordinationService}.
 */
public interface CoordinationProtocolPort {

    /**
     * Initiates a new coordination between two agents.
     *
     * @param requesterAgentId the agent initiating the meeting
     * @param inviteeAgentId   the agent being invited
     * @param lookAheadRange   the date range to search for availability
     * @param durationMinutes  desired meeting length
     * @param title            meeting title
     * @param timezone         timezone for event creation
     * @return the new CoordinationId
     */
    CoordinationId initiate(AgentId requesterAgentId, AgentId inviteeAgentId,
            TimeRange lookAheadRange, int durationMinutes,
            String title, String timezone);

    /**
     * Returns the available slots for a coordination (after matching).
     */
    List<TimeSlot> getAvailableSlots(CoordinationId coordinationId);

    /**
     * Records the user's slot selection and advances the coordination.
     *
     * @param coordinationId the coordination session
     * @param selectedSlot   the slot chosen by the invitee
     */
    void selectSlot(CoordinationId coordinationId, TimeSlot selectedSlot);

    /**
     * Handles an approval decision from an agent (invitee or requester).
     *
     * @param coordinationId the coordination session
     * @param agentId        the agent who approved/rejected
     * @param approved       true if approved, false if rejected
     */
    void handleApproval(CoordinationId coordinationId, AgentId agentId, boolean approved);

    /**
     * Advances an existing coordination to the next state based on an external
     * event.
     *
     * @param coordinationId the coordination to advance
     * @param toState        the target state
     * @param reason         human-readable description
     */
    void advance(CoordinationId coordinationId, CoordinationState toState, String reason);

    /**
     * Terminates a coordination in FAILED or REJECTED state.
     *
     * @param coordinationId the coordination to terminate
     * @param reason         why it was terminated
     */
    void terminate(CoordinationId coordinationId, String reason);
}
