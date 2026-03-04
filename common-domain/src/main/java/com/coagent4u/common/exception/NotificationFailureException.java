package com.coagent4u.common.exception;

/**
 * Thrown when sending a notification to the user fails (e.g. Slack API error).
 */
public class NotificationFailureException extends RuntimeException {

    public NotificationFailureException(String message) {
        super(message);
    }

    public NotificationFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
