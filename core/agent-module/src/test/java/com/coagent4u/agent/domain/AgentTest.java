package com.coagent4u.agent.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.coagent4u.shared.AgentId;
import com.coagent4u.shared.UserId;

class AgentTest {

    @Test
    void creation_isActive() {
        Agent agent = new Agent(AgentId.generate(), UserId.generate());
        assertTrue(agent.isActive());
        assertEquals(Agent.Status.ACTIVE, agent.getStatus());
    }

    @Test
    void deactivate_succeeds() {
        Agent agent = new Agent(AgentId.generate(), UserId.generate());
        agent.deactivate();
        assertFalse(agent.isActive());
        assertEquals(Agent.Status.INACTIVE, agent.getStatus());
    }

    @Test
    void deactivate_alreadyInactive_rejects() {
        Agent agent = new Agent(AgentId.generate(), UserId.generate());
        agent.deactivate();
        assertThrows(IllegalStateException.class, agent::deactivate);
    }

    @Test
    void activate_afterDeactivate() {
        Agent agent = new Agent(AgentId.generate(), UserId.generate());
        agent.deactivate();
        agent.activate();
        assertTrue(agent.isActive());
    }
}
