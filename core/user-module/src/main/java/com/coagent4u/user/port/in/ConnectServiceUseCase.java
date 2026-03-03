package com.coagent4u.user.port.in;

import java.time.Instant;

import com.coagent4u.shared.UserId;

/**
 * Inbound port — links a user's Google Calendar OAuth tokens to their account.
 */
public interface ConnectServiceUseCase {
    /**
     * @param userId                the user to connect
     * @param serviceType           e.g. "GOOGLE_CALENDAR"
     * @param encryptedToken        AES-256-GCM ciphertext of the access token
     * @param encryptedRefreshToken AES-256-GCM ciphertext of the refresh token
     * @param tokenExpiresAt        when the access token expires (may be null if
     *                              unknown)
     */
    void connect(UserId userId, String serviceType,
            String encryptedToken, String encryptedRefreshToken, Instant tokenExpiresAt);
}
