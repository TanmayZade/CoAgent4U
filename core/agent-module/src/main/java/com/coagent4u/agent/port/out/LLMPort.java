package com.coagent4u.agent.port.out;

import java.util.Optional;

import com.coagent4u.shared.AgentId;

/**
 * Outbound port — LLM-based intent classification (Tier 2 fallback).
 * Implemented in llm-module (GroqLLMAdapter).
 */
public interface LLMPort {
    /**
     * @param agentId the agent making the request (for model routing)
     * @param rawText the user's message
     * @return classified intent type string (e.g. "ADD_EVENT"), or empty on failure
     */
    Optional<String> classifyIntent(AgentId agentId, String rawText);

    /**
     * Generates a human-readable summary of a schedule.
     *
     * @param agentId      the agent
     * @param scheduleJson JSON representation of upcoming events
     * @return natural-language summary
     */
    String summarizeSchedule(AgentId agentId, String scheduleJson);
}
