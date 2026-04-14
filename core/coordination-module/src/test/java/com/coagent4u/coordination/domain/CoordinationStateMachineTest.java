package com.coagent4u.coordination.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.CoordinationId;

class CoordinationStateMachineTest {

    private final CoordinationStateMachine sm = new CoordinationStateMachine();

    @Test
    void initiated_to_checkingA_allowed() {
        assertDoesNotThrow(() -> sm.validateTransition(
                CoordinationState.INITIATED, CoordinationState.CHECKING_AVAILABILITY_A));
    }

    @Test
    void initiated_to_completed_rejected() {
        assertThrows(IllegalStateException.class,
                () -> sm.validateTransition(CoordinationState.INITIATED, CoordinationState.COMPLETED));
    }

    @Test
    void anyState_to_failed_allowed() {
        for (CoordinationState state : CoordinationState.values()) {
            if (state.isTerminal())
                continue;
            assertDoesNotThrow(() -> sm.validateTransition(state, CoordinationState.FAILED),
                    "Should be able to transition from " + state + " to FAILED");
        }
    }

    @Test
    void terminalStates_block_allTransitions() {
        for (CoordinationState terminal : EnumSet.of(
                CoordinationState.COMPLETED, CoordinationState.REJECTED, CoordinationState.FAILED)) {
            for (CoordinationState target : CoordinationState.values()) {
                assertThrows(IllegalStateException.class,
                        () -> sm.validateTransition(terminal, target),
                        terminal + " should not transition to " + target);
            }
        }
    }

    @Test
    void fullHappyPath() {
        CoordinationState[] path = {
                CoordinationState.INITIATED,
                CoordinationState.CHECKING_AVAILABILITY_A,
                CoordinationState.CHECKING_AVAILABILITY_B,
                CoordinationState.MATCHING,
                CoordinationState.PROPOSAL_GENERATED,
                CoordinationState.AWAITING_APPROVAL_B,
                CoordinationState.AWAITING_APPROVAL_A,
                CoordinationState.APPROVED_BY_BOTH,
                CoordinationState.CREATING_EVENT_A,
                CoordinationState.CREATING_EVENT_B,
                CoordinationState.COMPLETED
        };
        for (int i = 0; i < path.length - 1; i++) {
            final int idx = i;
            assertDoesNotThrow(() -> sm.validateTransition(path[idx], path[idx + 1]),
                    "Transition " + path[idx] + " → " + path[idx + 1] + " should be allowed");
        }
    }

    @Test
    void rejectionPath() {
        assertDoesNotThrow(() -> sm.validateTransition(
                CoordinationState.AWAITING_APPROVAL_B, CoordinationState.REJECTED));
        assertDoesNotThrow(() -> sm.validateTransition(
                CoordinationState.AWAITING_APPROVAL_A, CoordinationState.REJECTED));
    }
}

class CoordinationTest {

    @Test
    void creation_setsInitiatedState() {
        Coordination c = new Coordination(CoordinationId.generate(), AgentId.generate(), AgentId.generate(), 60);
        assertEquals(CoordinationState.INITIATED, c.getState());
        assertFalse(c.isTerminal());
        assertEquals(1, c.getStateLog().size()); // initial log entry
    }

    @Test
    void transition_logsStateChange() {
        Coordination c = new Coordination(CoordinationId.generate(), AgentId.generate(), AgentId.generate(), 60);
        c.transition(CoordinationState.CHECKING_AVAILABILITY_A, "Checking A");
        assertEquals(CoordinationState.CHECKING_AVAILABILITY_A, c.getState());
        assertEquals(2, c.getStateLog().size());
    }

    @Test
    void transition_toTerminal_setsCompletedAt() {
        Coordination c = new Coordination(CoordinationId.generate(), AgentId.generate(), AgentId.generate(), 60);
        c.transition(CoordinationState.FAILED, "Error occurred");
        assertTrue(c.isTerminal());
        assertNotNull(c.getCompletedAt());
    }

    @Test
    void transition_invalidTransition_throws() {
        Coordination c = new Coordination(CoordinationId.generate(), AgentId.generate(), AgentId.generate(), 60);
        assertThrows(IllegalStateException.class,
                () -> c.transition(CoordinationState.COMPLETED, "Skip"));
    }
}
