package com.coagent4u.common.exception;

/**
 * Thrown when the LLM service (Groq) is unavailable or returns an error.
 * The caller should fall back to Tier 1 rule-based parsing.
 */
public class LLMUnavailableException extends RuntimeException {

    public LLMUnavailableException(String message) {
        super(message);
    }

    public LLMUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
