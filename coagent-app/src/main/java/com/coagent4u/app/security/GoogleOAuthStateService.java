package com.coagent4u.app.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;

/**
 * Creates and validates signed JWT state tokens for Google OAuth.
 * The state token embeds userId + nonce + timestamp to prevent CSRF.
 *
 * <p>Uses the same signing key as JWT sessions but with a
 * short expiration (10 minutes).</p>
 */
public class GoogleOAuthStateService {

    private static final long STATE_TOKEN_TTL_SECONDS = 600; // 10 minutes

    private final SecretKey key;

    public GoogleOAuthStateService(String jwtSecret) {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * DTO containing decoded state information.
     */
    public record StateInfo(UUID userId, String returnTo) {}

    /**
     * Creates a signed state token containing the userId and an optional return URL.
     */
    public String createStateToken(UUID userId, String returnTo) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString()) // nonce
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(STATE_TOKEN_TTL_SECONDS)))
                .signWith(key);
                
        if (returnTo != null && !returnTo.isBlank()) {
            builder.claim("returnTo", returnTo);
        }
        
        return builder.compact();
    }

    /**
     * Validates the state token and returns the parsed state information.
     * Returns null if the state token is invalid or expired.
     */
    public StateInfo validateStateToken(String stateToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(stateToken)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String returnTo = claims.get("returnTo", String.class);
            return new StateInfo(userId, returnTo);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
