package com.coagent4u.common.exception;

/**
 * Thrown when an OAuth token has expired and automatic refresh failed.
 * Signals that the user needs to re-authenticate.
 */
public class TokenExpiredException extends RuntimeException {

    private final String serviceType;

    public TokenExpiredException(String serviceType, String message) {
        super(serviceType + " token expired: " + message);
        this.serviceType = serviceType;
    }

    public TokenExpiredException(String serviceType, String message, Throwable cause) {
        super(serviceType + " token expired: " + message, cause);
        this.serviceType = serviceType;
    }

    public String getServiceType() {
        return serviceType;
    }
}
