package com.coagent4u.coordination.port.in;

import com.coagent4u.coordination.domain.CoordinationState;
import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;
import com.coagent4u.shared.TimeRange;

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
