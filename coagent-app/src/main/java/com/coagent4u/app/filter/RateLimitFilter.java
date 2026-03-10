package com.coagent4u.app.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.coagent4u.app.security.AuthenticatedUser;
import com.coagent4u.security.CaffeineRateLimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiting filter keyed by userId + endpoint.
 * Uses the existing {@link CaffeineRateLimiter} (100 req/min per key).
 *
 * <p>Only applies to authenticated requests. Unauthenticated requests
 * are skipped (they will be handled by the auth filter).</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final CaffeineRateLimiter rateLimiter;

    public RateLimitFilter(CaffeineRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        AuthenticatedUser user = AuthenticatedUser.from(request);
        if (user == null) {
            // Not yet authenticated — skip rate limiting
            filterChain.doFilter(request, response);
            return;
        }

        String key = user.userId().toString() + ":" + request.getRequestURI();

        if (!rateLimiter.tryAcquire(key)) {
            log.warn("Rate limit exceeded for user={} endpoint={}", user.userId(), request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again later.\"}");
            return;
        }

        // Add remaining limit to response header
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining(key)));

        filterChain.doFilter(request, response);
    }
}
