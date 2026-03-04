package com.coagent4u.common.exception;

/**
 * Thrown when an external service (Google Calendar, Slack, Groq) is
 * unavailable,
 * rate-limited, or returning server errors.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ExternalServiceUnavailableException(String serviceName, String message) {
        super(serviceName + ": " + message);
        this.serviceName = serviceName;
    }

    public ExternalServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(serviceName + ": " + message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
