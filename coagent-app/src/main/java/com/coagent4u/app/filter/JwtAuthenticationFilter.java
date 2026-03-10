package com.coagent4u.app.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.coagent4u.app.security.AuthenticatedUser;
import com.coagent4u.security.JwtTokenBlacklist;
import com.coagent4u.security.JwtValidator;
import com.coagent4u.security.JwtValidator.JwtClaims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT authentication filter. Reads JWT from {@code coagent_session} HTTPOnly cookie,
 * validates it, checks blacklist, and sets {@link AuthenticatedUser} in request attribute.
 *
 * <p>Public endpoints (Slack callbacks, health, actuator) are skipped.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String COOKIE_NAME = "coagent_session";

    /** Paths that do NOT require authentication. */
    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/auth/slack/start",
            "/auth/slack/callback",
            "/integrations/google/callback",
            "/api/health",
            "/actuator",
            "/api/slack/events",
            "/api/slack/interactivity"
    );

    private final JwtValidator jwtValidator;
    private final JwtTokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtValidator jwtValidator, JwtTokenBlacklist tokenBlacklist) {
        this.jwtValidator = jwtValidator;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token == null) {
            // No cookie — let the request through; controllers decide if auth is required
            filterChain.doFilter(request, response);
            return;
        }

        Optional<JwtClaims> claimsOpt = jwtValidator.validateFull(token);
        if (claimsOpt.isEmpty()) {
            log.debug("Invalid or expired JWT");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or expired session\"}");
            response.setContentType("application/json");
            return;
        }

        JwtClaims claims = claimsOpt.get();

        // Check blacklist
        if (claims.jti() != null && tokenBlacklist.isRevoked(claims.jti())) {
            log.debug("Revoked JWT jti={}", claims.jti());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Session has been revoked\"}");
            response.setContentType("application/json");
            return;
        }

        // Set authenticated user in request attribute
        AuthenticatedUser user = new AuthenticatedUser(
                claims.userId(),
                claims.username(),
                claims.pendingRegistration());
        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
