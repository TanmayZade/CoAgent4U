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

    public String issue(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
                .signWith(key)
                .compact();
    }
}
