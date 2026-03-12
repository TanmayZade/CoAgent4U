package com.coagent4u.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues HS256-signed JWTs with userId subject, iat, exp (24h default), and
 * jti.
 * No PII. No roles/permissions (MVP). Fails fast if signing key is missing or
 * too short.
 */
public class JwtIssuer {

    private final SecretKey key;
    private final long expiryMinutes;

    /** Pending registration tokens expire in 10 minutes. */
    private static final long PENDING_EXPIRY_MINUTES = 10;

    public JwtIssuer(String secret, long expiryMinutes) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing key must not be blank — set COAGENT_SECURITY_JWT_SECRET env var");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT signing key must be >= 32 bytes for HS256");
        }
        if (expiryMinutes <= 0) {
            throw new IllegalStateException("JWT expiry must be > 0 minutes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    /** Backward-compatible: issues JWT with userId only. */
    public String issue(UUID userId) {
        return issue(userId, null, false);
    }

    /**
     * Issues a full session JWT with claims.
     *
     * @param userId              the user's UUID (subject)
     * @param username            display username (may be null for legacy calls)
     * @param pendingRegistration true if user has not yet completed onboarding
     * @return signed JWT string
     */
    public String issue(UUID userId, String username, boolean pendingRegistration) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("pending_registration", pendingRegistration)
                .claim("auth_provider", "slack")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiryMinutes * 60)));
        if (username != null) {
            builder.claim("username", username);
        }
        return builder.signWith(key).compact();
    }

    /**
     * Issues a short-lived pending registration JWT containing Slack identity.
     * Expires in {@value #PENDING_EXPIRY_MINUTES} minutes — user must complete
     * onboarding within this window.
     *
     * <p>Slack identity is embedded as signed claims so it cannot be tampered
     * with by the client.</p>
     *
     * @param userId      temporary user ID (not yet persisted)
     * @param slackUserId Slack platform user ID
     * @param workspaceId Slack workspace / team ID
     * @param email       may be null
     * @param displayName may be null
     * @return signed JWT string with 10-minute expiry
     */
    public String issuePending(UUID userId, String slackUserId,
            String workspaceId, String email, String displayName) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("pending_registration", true)
                .claim("auth_provider", "slack")
                .claim("slack_user_id", slackUserId)
                .claim("workspace_id", workspaceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(PENDING_EXPIRY_MINUTES * 60)));
        if (email != null) {
            builder.claim("email", email);
        }
        if (displayName != null) {
            builder.claim("display_name", displayName);
        }
        return builder.signWith(key).compact();
    }
}

