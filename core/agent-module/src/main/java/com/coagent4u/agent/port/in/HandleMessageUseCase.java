package com.coagent4u.agent.port.in;

import com.coagent4u.shared.AgentId;

/**
 * Inbound port — processes a Slack message sent by a user to their agent.
 */
public interface HandleMessageUseCase {
    /**
     * @param agentId the agent receiving the message
     * @param rawText the raw text from Slack
     */
    void handleMessage(AgentId agentId, String rawText);
}
