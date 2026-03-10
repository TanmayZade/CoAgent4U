package com.coagent4u.security;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * In-memory JWT token blacklist backed by Caffeine cache.
 * Entries automatically expire after the configured JWT TTL.
 *
 * <p>For MVP single-instance deployment, this is sufficient. The application
 * layer is responsible for also persisting revocations to the {@code revoked_tokens}
 * table and warming this cache on startup.</p>
 */
public class JwtTokenBlacklist {

    private final Cache<String, Boolean> revokedTokens;

    public JwtTokenBlacklist(long jwtExpiryMinutes) {
        this.revokedTokens = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(jwtExpiryMinutes))
                .maximumSize(100_000)
                .build();
    }

    /**
     * Marks a token (by its jti) as revoked.
     */
    public void revoke(String jti) {
        if (jti != null && !jti.isBlank()) {
            revokedTokens.put(jti, Boolean.TRUE);
        }
    }

    /**
     * Returns true if the token (by its jti) has been revoked.
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return revokedTokens.getIfPresent(jti) != null;
    }
}
