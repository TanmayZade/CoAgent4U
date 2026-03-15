package com.coagent4u.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Validates and parses HS256-signed JWTs.
 */
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the JWT and returns the userId from the subject claim.
     * Returns empty if the token is invalid, expired, or malformed.
     * Backward-compatible method.
     */
    public Optional<UUID> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Full JWT claims including username, jti, pending_registration,
     * auth_provider, and optional Slack identity fields.
     */
    public record JwtClaims(
            UUID userId,
            String username,
            String jti,
            boolean pendingRegistration,
            String authProvider,
            String slackUserId,
            String workspaceId,
            String workspaceName,
            String workspaceDomain,
            String email,
            String displayName,
            String avatarUrl,
            String dmChannelId,
            Instant issuedAt,
            Instant expiry) {
    }

    /**
     * Parses the JWT and returns full claims.
     * Returns empty if the token is invalid, expired, or malformed.
     * All optional claims are null-safe — missing claims return null.
     */
    public Optional<JwtClaims> validateFull(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            String jti = claims.getId();
            Boolean pendingReg = claims.get("pending_registration", Boolean.class);

            // Extended claims — all null-safe
            String authProvider = claims.get("auth_provider", String.class);
            String slackUserId = claims.get("slack_user_id", String.class);
            String workspaceId = claims.get("workspace_id", String.class);
            String workspaceName = claims.get("workspace_name", String.class);
            String workspaceDomain = claims.get("workspace_domain", String.class);
            String email = claims.get("email", String.class);
            String displayName = claims.get("display_name", String.class);
            String avatarUrl = claims.get("avatar_url", String.class);
            String dmChannelId = claims.get("slack_dm_channel_id", String.class);
            Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
            Instant expiry = claims.getExpiration() != null ? claims.getExpiration().toInstant() : null;

            return Optional.of(new JwtClaims(
                    userId,
                    username,
                    jti,
                    pendingReg != null && pendingReg,
                    authProvider,
                    slackUserId,
                    workspaceId,
                    workspaceName,
                    workspaceDomain,
                    email,
                    displayName,
                    avatarUrl,
                    dmChannelId,
                    issuedAt,
                    expiry));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

