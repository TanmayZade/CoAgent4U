package com.coagent4u.user.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a connection to an external service (e.g. Google
 * Calendar).
 * Stores encrypted OAuth tokens — never plaintext.
 */
public class ServiceConnection {
    private final UUID connectionId;
    private final String serviceType; // e.g. "GOOGLE_CALENDAR"
    private String encryptedToken; // AES-256-GCM ciphertext
    private String encryptedRefreshToken; // AES-256-GCM ciphertext
    private Instant tokenExpiresAt;
    private UserConnectionStatus status;
    private final Instant connectedAt;
    private Instant disconnectedAt;

    public ServiceConnection(UUID connectionId, String serviceType,
            String encryptedToken, String encryptedRefreshToken,
            Instant tokenExpiresAt) {
        this.connectionId = Objects.requireNonNull(connectionId);
        this.serviceType = Objects.requireNonNull(serviceType);
        this.encryptedToken = Objects.requireNonNull(encryptedToken);
        this.encryptedRefreshToken = Objects.requireNonNull(encryptedRefreshToken);
        this.tokenExpiresAt = tokenExpiresAt;
        this.status = UserConnectionStatus.CONNECTED;
        this.connectedAt = Instant.now();
    }

    public void revoke() {
        this.status = UserConnectionStatus.REVOKED;
        this.disconnectedAt = Instant.now();
    }

    public void markExpired() {
        this.status = UserConnectionStatus.EXPIRED;
    }

    public void refreshToken(String newEncryptedToken, String newEncryptedRefreshToken, Instant newExpiresAt) {
        this.encryptedToken = Objects.requireNonNull(newEncryptedToken);
        this.encryptedRefreshToken = Objects.requireNonNull(newEncryptedRefreshToken);
        this.tokenExpiresAt = newExpiresAt;
        this.status = UserConnectionStatus.CONNECTED;
    }

    public boolean isActive() {
        return status == UserConnectionStatus.CONNECTED;
    }

    // Getters
    public UUID getConnectionId() {
        return connectionId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }
}
