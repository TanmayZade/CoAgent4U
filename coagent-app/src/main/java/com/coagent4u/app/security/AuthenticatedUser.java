package com.coagent4u.app.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Represents an authenticated user extracted from the JWT cookie.
 * Stored in request attribute {@code AUTHENTICATED_USER} by {@link JwtAuthenticationFilter}.
 *
 * <p>Frontend uses {@code username}; backend internally uses {@code userId}.</p>
 */
public record AuthenticatedUser(
        UUID userId,
        String username,
        boolean pendingRegistration) {

    public static final String REQUEST_ATTRIBUTE = "AUTHENTICATED_USER";

    /**
     * Extracts the AuthenticatedUser from the request attribute.
     * Returns null if not authenticated.
     */
    public static AuthenticatedUser from(HttpServletRequest request) {
        return (AuthenticatedUser) request.getAttribute(REQUEST_ATTRIBUTE);
    }
}
